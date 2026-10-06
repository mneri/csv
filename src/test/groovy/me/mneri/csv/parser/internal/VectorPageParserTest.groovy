/*
 * Copyright 2018 Massimo Neri <hello@mneri.me>
 *
 * This file is part of mneri/csv.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.mneri.csv.parser.internal

import me.mneri.csv.exception.UnexpectedCharacterException
import me.mneri.csv.extension.internal.Extensions
import me.mneri.csv.format.Format
import spock.lang.Requires
import spock.lang.Specification

import static me.mneri.csv.format.Format.*

/**
 * The parser is tested with mock formats: each test says what the format answers to each character, so the tests
 * depend only on how the parser carries out the actions, not on the rules of a real CSV dialect.
 */
@Requires({ Extensions.SIMD_SUPPORTED }) // The test JVM needs --add-modules=jdk.incubator.vector, as build.gradle sets
class VectorPageParserTest extends Specification {
    // Rules of a mock format: an uppercase letter starts a field, '.' ends it, ';' ends the field and the line, and the
    // end of file stops the parser. Any other character asks for nothing.
    static final Closure<Integer> FIELDS = { int s, int c ->
        if (c == -1) return STP
        if (c == ch('.')) return EFH
        if (c == ch(';')) return EFH | ELH
        return Character.isUpperCase(c) ? SFH : 0
    }

    // A bitmask that selects every character, the padding after the end of the page included
    static final Closure<Long> EVERY_CHARACTER = { int s, char[] buf, int offset -> -1L }

    // A bitmask that selects every character but 'x'
    static final Closure<Long> ALL_BUT_X = { int s, char[] buf, int offset ->
        long bitmask = 0L
        for (int i = 0; i < 64; i++) {
            if (buf[offset + i] != ('x' as char)) {
                bitmask |= 1L << i
            }
        }
        return bitmask
    }

    // What the parser gave the mock format, in order: [state, character], with "EOF" for the end of file
    def calls = []

    // The bitmasks the parser asked the mock format for, in order: [state, offset]
    def strides = []

    def "starts from the base state of the format, gives it every character and the end of file, and passes back each state"() {
        given: "a format whose state counts from 100"
        def format = format(base: 100) { int s, int c -> c == -1 ? STP : s + 1 }

        when:
        parse(format, "ab")

        then:
        calls == [[100, "a"], [101, "b"], [102, "EOF"]]
    }

    def "starts and ends fields and lines where the format says: #description"() {
        expect:
        parse(format(FIELDS), input) == expected

        where:
        description                    | input       || expected
        "no input"                     | ""          || []
        "one field"                    | "Abc;"      || [["Abc"]]
        "two fields"                   | "Abc.De;"   || [["Abc", "De"]]
        "two lines"                    | "Ab;Cd;"    || [["Ab"], ["Cd"]]
        "characters outside of fields" | "xAb.yCd;z" || [["Ab", "Cd"]]
    }

    def "ends a field one character back: #description"() {
        given: "a format where ',' ends the field before the previous character, and the line"
        def format = format { int s, int c ->
            if (c == -1) return STP
            if (c == ch(',')) return EFB | ELH
            return Character.isUpperCase(c) ? SFH : 0
        }

        expect:
        parse(format, input) == expected

        where:
        description | input      || expected
        "one line"  | 'Abc",'    || [["Abc"]]
        "two lines" | 'Ab",Cd",' || [["Ab"], ["Cd"]]
    }

    def "ends a line one character back, and gives that character to the format again for the next line, in pages of #pageSize"() {
        given: "a format where the character after a CR ends the line before it, and is replayed"
        def format = format { int s, int c ->
            if (s == 1) return ELB | RPL
            if (c == ch('\r')) return 1 | EFH
            if (c == -1) return EFH | ELH | STP
            return Character.isUpperCase(c) ? SFH : 0
        }

        expect:
        parse(format, "Ab\rCd", pageSize) == [["Ab"], ["Cd"]]
        calls*.get(1) == ["A", "b", "\r", "C", "C", "d", "EOF"]

        where:
        pageSize << [4, 4_096] // In pages of 4, the replayed character is the last of the page: its line is carried over
    }

    def "gives the end of file to the format again when it asks, and stops when the format says so"() {
        given: "a format that replays the end of file once, then stops"
        def format = format { int s, int c ->
            if (c == -1) return s == 2 ? STP : 2 | EFH | ELH | RPL
            return Character.isUpperCase(c) ? SFH : 0
        }

        expect:
        parse(format, "Ab") == [["Ab"]]
        calls*.get(1) == ["A", "b", "EOF", "EOF"]
    }

    def "drops the character before the one the format marks: #description"() {
        given: "a format where the second of two quotes drops the first"
        def format = format { int s, int c ->
            if (c == ch('"')) return s == 1 ? RMB : 1
            if (c == -1) return STP
            if (c == ch('.')) return EFH
            if (c == ch(';')) return EFH | ELH
            return Character.isUpperCase(c) ? SFH : 0
        }

        expect:
        parse(format, input, pageSize) == expected

        where:
        description                                | input             | pageSize || expected
        "one character"                            | 'Ab""c;'          | 4_096    || [['Ab"c']]
        "two characters in a row"                  | 'A""""b;'         | 4_096    || [['A""b']]
        "in several fields and lines"              | 'A""b.C""d;E""f;' | 4_096    || [['A"b', 'C"d'], ['E"f']]
        "dropped on the page before the line ends" | 'Ab;C""d;'        | 6        || [["Ab"], ['C"d']]
        "marked at the start of the next page"     | 'Ab;C""d;'        | 5        || [["Ab"], ['C"d']]
    }

    def "reports an error at its position in the stream, after the lines before it: #description"() {
        given: "a format where '!' and the end of file are errors"
        def format = format { int s, int c ->
            if (c == -1 || c == ch('!')) return ERH
            if (c == ch(';')) return EFH | ELH
            return Character.isUpperCase(c) ? SFH : 0
        }
        def lines = []

        when:
        parse(format, input, pageSize, lines)

        then:
        def e = thrown(UnexpectedCharacterException)
        e.position == position
        lines == before
        calls.last()[1] == last // The format gets nothing after the error

        where:
        description                           | input       | pageSize || before           | position | last
        "on the first page"                   | "Ab;Cd!e;"  | 4_096    || [["Ab"]]         | 5        | "!"
        "on a later page"                     | "Ab;Cd;Ef!" | 4        || [["Ab"], ["Cd"]] | 8        | "!"
        "at the end of file"                  | "Ab;Cd"     | 4_096    || [["Ab"]]         | 5        | "EOF"
        "at the end of file, on a later page" | "Ab;Cd"     | 4        || [["Ab"]]         | 5        | "EOF"
    }

    def "carries the line in progress and the state of the format over to the next page, in pages of #pageSize"() {
        given: "a format whose state counts the characters it has been given"
        def format = format { int s, int c -> (s + 1) | FIELDS(s, c) }

        expect:
        parse(format, "Ab;Cde.Fg;", pageSize) == [["Ab"], ["Cde", "Fg"]]
        calls*.get(0) == (0..10).toList() // Every character once, in order, then the end of file
        calls*.get(1) == ["A", "b", ";", "C", "d", "e", ".", "F", "g", ";", "EOF"]

        where:
        pageSize << [7, 8, 9, 10, 11, 4_096]
    }

    def "at the end of a page that isn't the last, leaves the line in progress as the tail: #description"() {
        given:
        def page = new Page(4)
        page.fill(new StringReader(input))
        def format = format(FIELDS)

        when:
        new VectorPageParser({ -> format } as Format.Provider).parse(page)

        then:
        page.lineCount() == lines
        page.tail == tail
        !page.isLast()
        !calls*.get(1).contains("EOF")

        where:
        description                        | input      || lines | tail
        "a line in progress"               | "Ab;Cdef;" || 1     | 3
        "a line that fills the whole page" | "Abcdef;"  || 0     | 0
    }

    def "asks the format for a bitmask every 64 characters, with the state at that point"() {
        given: "a format whose state counts the characters it has been given"
        def format = format { int s, int c -> (s + 1) | FIELDS(s, c) }

        when:
        parse(format, "A" + "b" * 130 + ";")

        then:
        strides == [[0, 0], [64, 64], [128, 128]]
    }

    def "gives the format only the characters the bitmask selects, and keeps the others in the fields"() {
        given: "a bitmask that leaves out every x"
        def format = format(bitmask: ALL_BUT_X, FIELDS)

        expect:
        parse(format, "xAxx.xCx;x") == [["Axx", "Cx"]]
        calls*.get(1) == ["A", ".", "C", ";", "EOF"]
    }

    def "after carrying a line over, asks for the first bitmask of the page right after the carried characters"() {
        given: "a format whose state counts the characters it has been given"
        def format = format { int s, int c -> (s + 1) | FIELDS(s, c) }

        when:
        parse(format, "Ab;Cde.Fg;", 8)

        then:
        strides == [[0, 0], [8, 5]] // Page two starts with the 5 characters "Cde.F", already given to the format
    }

    /**
     * Return a mock format. The rules get the state (without the actions) and the character, -1 at the end of file,
     * and return the next state with the actions; the bitmask selects every character, unless the options say
     * otherwise. Every call is recorded in {@link #calls} and {@link #strides}.
     */
    private Format format(Map options = [:], Closure<Integer> rules) {
        def calls = this.calls
        def strides = this.strides
        int initial = options.base ?: 0
        Closure<Long> select = options.bitmask ?: EVERY_CHARACTER
        Stub(Format) {
            base() >> initial
            bitmask(_, _, _) >> { int s, char[] buf, int offset ->
                strides << [s & 0xFFFF, offset]
                select(s & 0xFFFF, buf, offset)
            }
            consumeSlow(_, _) >> { int s, int c ->
                calls << [s & 0xFFFF, c == -1 ? "EOF" : String.valueOf((char) c)]
                rules(s & 0xFFFF, c)
            }
        }
    }

    private static List<List<String>> parse(Format format, String input, int pageSize = 4_096, List<List<String>> out = []) {
        new PageDriver(new VectorPageParser({ -> format } as Format.Provider)).parse(input, pageSize, out)
    }

    private static int ch(String s) {
        return s.charAt(0)
    }
}
