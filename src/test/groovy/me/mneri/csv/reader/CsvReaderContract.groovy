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

package me.mneri.csv.reader

import me.mneri.csv.deserializer.Deserializer
import me.mneri.csv.deserializer.StringListDeserializer
import me.mneri.csv.exception.UnexpectedCharacterException
import me.mneri.csv.format.Format
import me.mneri.csv.format.Rfc4180FullyRelaxedFormat
import me.mneri.csv.format.Rfc4180StrictFormat
import me.mneri.csv.hint.Hint
import me.mneri.csv.reader.CsvReader.Configuration
import spock.lang.Specification

import java.nio.BufferOverflowException

/**
 * The behaviour that every {@link CsvReader} shares. The test of each reader extends this class and says how to open
 * that reader; the features below then run against it.
 * <p>
 * The features run with both parsers: the hints choose the scalar parser, or the vector parser when the JVM supports
 * the Vector API, as the test JVM does.
 */
abstract class CsvReaderContract extends Specification {
    // The hints that choose each parser
    static final Map<String, Long> PARSERS = [vector: 0L, scalar: Hint.TINY_FIELDS]

    // The readers opened by the feature: cleanup() closes them, so no background thread outlives its feature
    List<CsvReader> readers = []

    /**
     * Return a new reader of the stream.
     */
    abstract <T> CsvReader<T> newReader(Reader stream, Format.Provider<? extends Format> format,
                                        Deserializer<T> deserializer, Configuration config)

    def cleanup() {
        readers*.close()
    }

    def "reads every line, in order, with the #parser parser and pages of #maxLineSize characters"() {
        given:
        def csv = randomCsv(maxLineSize)

        expect:
        readAll(open(csv.text, parser: parser, maxLineSize: maxLineSize)) == csv.lines

        where:
        [parser, maxLineSize] << [PARSERS.keySet(), [16, 17, 64, 4_096]].combinations()
    }

    def "accepts a line of #length characters in pages of 100, line break included: #accepted (#parser parser)"() {
        given:
        def reader = open("b\n" + "a" * (length - 1) + "\nc\n", parser: parser, maxLineSize: 100)
        def lines = []
        def overflow = false

        when:
        try {
            while (reader.hasNext()) {
                lines << reader.next()
            }
        } catch (BufferOverflowException ignored) {
            overflow = true
        }

        then:
        overflow == !accepted
        lines == (accepted ? [["b"], ["a" * (length - 1)], ["c"]] : [["b"]])

        where:
        [parser, length] << [PARSERS.keySet(), [99, 100, 101]].combinations()
        accepted = length <= 100
    }

    def "an empty stream has no lines (#parser parser)"() {
        expect:
        readAll(open("", parser: parser)) == []

        where:
        parser << PARSERS.keySet()
    }

    def "hasNext() can be called any number of times, and next() works without it (#parser parser)"() {
        given: "pages of four characters, so each page holds at most two lines"
        def reader = open("a\nb\nc\nd", parser: parser, maxLineSize: 4)

        expect:
        reader.hasNext()
        reader.hasNext()
        reader.next() == ["a"]
        reader.next() == ["b"]
        reader.hasNext()
        reader.hasNext()
        reader.next() == ["c"]
        reader.next() == ["d"]
        !reader.hasNext()
        !reader.hasNext()

        where:
        parser << PARSERS.keySet()
    }

    def "next() throws NoSuchElementException after the last line (#parser parser)"() {
        given:
        def reader = open("a\nb", parser: parser)
        reader.next()
        reader.next()

        when:
        reader.next()

        then:
        thrown(NoSuchElementException)
        !reader.hasNext()

        where:
        parser << PARSERS.keySet()
    }

    def "a format error is thrown after the lines before it, at its position in the stream (#parser parser, pages of #maxLineSize characters)"() {
        given: "100 lines, then a quote inside an unquoted field"
        def valid = (1..100).collect { [it.toString(), "aaa"] }
        def text = valid*.join(",").join("\r\n") + '\r\nbb"b\r\n'
        def reader = open(text, format: Rfc4180StrictFormat.provider(), parser: parser, maxLineSize: maxLineSize)
        def lines = []

        when:
        while (reader.hasNext()) {
            lines << reader.next()
        }

        then:
        def e = thrown(UnexpectedCharacterException)
        e.position == text.indexOf('"')
        lines == valid

        when: "the client asks again"
        reader.hasNext()

        then: "the error is thrown again"
        thrown(UnexpectedCharacterException)

        where:
        [parser, maxLineSize] << [PARSERS.keySet(), [16, 4_096]].combinations()
    }

    def "an exception from the stream is thrown after the lines read before it: #failure.class.simpleName (#parser parser)"() {
        given: "a stream that fails after its first 100 characters"
        def expected = (1..1_000).collect { [it.toString(), "aaa"] }
        def stream = new RecordingReader(expected*.join(",").join("\n"), failAfter: 100, failure: failure)
        def reader = open(stream, parser: parser, maxLineSize: 16)
        def lines = []

        when:
        while (reader.hasNext()) {
            lines << reader.next()
        }

        then: "the exception is the stream's own, after the lines of the pages loaded before it"
        def e = thrown(Exception)
        e.is(failure)
        !lines.isEmpty()
        lines == expected.take(lines.size())

        where:
        [failure, parser] << [[new IOException("The disk is on fire"), new IllegalStateException("Broken stream")],
                              PARSERS.keySet()].combinations()
    }

    def "an exception from the deserializer is thrown by next() (#parser parser)"() {
        given:
        def failure = new IOException("Bad line")
        def deserializer = { line -> line.getString(0) == "b" ? { throw failure }() : line.getString(0) } as Deserializer
        def reader = open("a\nb\nc", deserializer: deserializer, parser: parser)

        when:
        def first = reader.next()
        reader.next()

        then:
        def e = thrown(IOException)
        e.is(failure)
        first == "a"

        where:
        parser << PARSERS.keySet()
    }

    def "close() closes the stream, after #linesRead lines (#parser parser)"() {
        given:
        def stream = new RecordingReader("a\nb\nc")
        def reader = open(stream, parser: parser, maxLineSize: 4)
        linesRead.times { reader.next() }

        when:
        reader.close()

        then:
        stream.closed

        where:
        [parser, linesRead] << [PARSERS.keySet(), [0, 1, 3]].combinations()
    }

    def "after close(), the reader can't be used, and closing it again has no effect (#parser parser)"() {
        given:
        def reader = open("a\nb", parser: parser)
        reader.next()
        reader.close()

        when:
        reader.hasNext()

        then:
        thrown(IllegalStateException)

        when:
        reader.next()

        then:
        thrown(IllegalStateException)

        when:
        reader.close()

        then:
        noExceptionThrown()

        where:
        parser << PARSERS.keySet()
    }

    /**
     * Return a new reader of the text, closed at the end of the feature. The options are {@code format} (fully
     * relaxed by default), {@code deserializer} (a list of strings by default), {@code parser} ({@code "vector"} by
     * default) and {@code maxLineSize}.
     */
    CsvReader open(Map options = [:], String text) {
        return open(options, new StringReader(text))
    }

    /**
     * Return a new reader of the stream, closed at the end of the feature, with the options of
     * {@link #open(Map, String)}.
     */
    CsvReader open(Map options = [:], Reader stream) {
        def config = Configuration.builder()
                .withMaxLineSize(options.maxLineSize ?: Configuration.DEFAULT_MAX_LINE_SIZE)
                .withHints(PARSERS[options.parser ?: "vector"])
                .build()
        def reader = newReader(stream, options.format ?: Rfc4180FullyRelaxedFormat.provider(),
                options.deserializer ?: new StringListDeserializer(), config)
        readers << reader
        return reader
    }

    static List readAll(CsvReader reader) {
        def lines = []
        while (reader.hasNext()) {
            lines << reader.next()
        }
        return lines
    }

    /**
     * Return 1,000 random lines, and the CSV text of them. Fields are quoted when they hold a comma, a quote or a line
     * break, and sometimes when they don't; lines end with LF, CR LF or CR, except the last one, which has no line
     * break. Every line, line break included, is shorter than {@code maxLineSize}: a line that ends with a lone CR
     * takes one more character to end, the one after the CR.
     * <p>
     * Empty fields are expected as {@code null}, as {@code RecycledLine.getString()} returns them.
     */
    static Map randomCsv(int maxLineSize, long seed = 42) {
        def random = new Random(seed)
        def alphabet = ["a", "b", " ", ",", '"', "\n", "\r"]
        def text = new StringBuilder()
        def lines = []
        1_000.times {
            def fields
            def encoded
            do {
                fields = (0..random.nextInt(3)).collect {
                    (0..<random.nextInt(4)).collect { alphabet[random.nextInt(alphabet.size())] }.join()
                }
                encoded = fields.collect { field ->
                    field.find(/[,"\r\n]/) || random.nextInt(4) == 0 ? '"' + field.replace('"', '""') + '"' : field
                }.join(",")
                if (encoded.isEmpty()) {
                    encoded = '""' // An empty line after a lone CR would merge into a CR LF
                }
            } while (encoded.length() + 2 >= maxLineSize) // 2: room for the longest line break, CR LF
            if (text.length() > 0) {
                text << ["\n", "\r\n", "\r"][random.nextInt(3)]
            }
            text << encoded
            lines << fields.collect { it ?: null }
        }
        return [text: text.toString(), lines: lines]
    }
}
