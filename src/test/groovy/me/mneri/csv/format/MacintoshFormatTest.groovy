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

package me.mneri.csv.format

import me.mneri.csv.exception.UnexpectedCharacterException
import spock.lang.Specification

class MacintoshFormatTest extends Specification {
    def driver = new FormatDriver(MacintoshFormat.provider())

    // Empty fields are empty strings: FormatDriver reports them as they are in the input. Lines end with CR only.

    def "empty input and blank lines: #description"() {
        expect:
        driver.parse(input) == expected

        where:
        description                        | input              || expected
        "empty input"                      | ""                 || []
        "CR only"                          | "\r"               || [[""]]
        "two CR"                           | "\r\r"             || [[""], [""]]
        "blank line between lines"         | "a\r\rb"           || [["a"], [""], ["b"]]
        "blank line at the end"            | "a\r\r"            || [["a"], [""]]
    }

    def "unquoted fields: #description"() {
        expect:
        driver.parse(input) == expected

        where:
        description                        | input              || expected
        "one character"                    | "a"                || [["a"]]
        "one field"                        | "abc"              || [["abc"]]
        "two fields"                       | "a,b"              || [["a", "b"]]
        "three fields"                     | "a,b,c"            || [["a", "b", "c"]]
        "only a comma"                     | ","                || [["", ""]]
        "only two commas"                  | ",,"               || [["", "", ""]]
        "empty last field"                 | "a,"               || [["a", ""]]
        "empty first field"                | ",a"               || [["", "a"]]
        "empty middle field"               | "a,,b"             || [["a", "", "b"]]
        "spaces are kept"                  | " a , b "          || [[" a ", " b "]]
        "tab is ordinary"                  | "a\tb,c"           || [["a\tb", "c"]]
        "punctuation below comma"          | "!#\$%&'()*+"      || [["!#\$%&'()*+"]]
        "control characters"               | "\u0000\u0001\u000B\u000C" || [["\u0000\u0001\u000B\u000C"]]
        "non-ASCII characters"             | "é,中文,😀"         || [["é", "中文", "😀"]]
        "U+FFFF is not the end of file"    | "a\uFFFFb,c"     || [["a\uFFFFb", "c"]]
        "long field"                       | "x" * 10_000       || [["x" * 10_000]]
    }

    def "line breaks: #description"() {
        expect:
        driver.parse(input) == expected

        where:
        description                        | input              || expected
        "CR between lines"                 | "a,b\rc,d"         || [["a", "b"], ["c", "d"]]
        "CR at the end"                    | "a,b\r"            || [["a", "b"]]
        "no line break at the end"         | "a,b\rc"           || [["a", "b"], ["c"]]
        "different number of fields"       | "a,b,c\rd\re,f"    || [["a", "b", "c"], ["d"], ["e", "f"]]
        "empty last field before CR"       | "a,\rb"            || [["a", ""], ["b"]]
        "comma right after a line break"   | "a\r,b"            || [["a"], ["", "b"]]
        "LF is an ordinary character"      | "a\nb,c"           || [["a\nb", "c"]]
        "LF alone is a field"              | "\n"               || [["\n"]]
        "CR LF: the LF starts the next line" | "a\r\nb"         || [["a"], ["\nb"]]
    }

    def "quoted fields: #description"() {
        expect:
        driver.parse(input) == expected

        where:
        description                        | input                  || expected
        "one quoted field"                 | '"a"'                  || [["a"]]
        "empty quoted field"               | '""'                   || [[""]]
        "quoted field with comma"          | '"a,b"'                || [["a,b"]]
        "quoted field with CR"             | '"a\rb"'               || [["a\rb"]]
        "quoted field with LF"             | '"a\nb"'               || [["a\nb"]]
        "quoted field with CR LF"          | '"a\r\nb"'             || [["a\r\nb"]]
        "quoted first field"               | '"a",b,c'              || [["a", "b", "c"]]
        "quoted middle field"              | 'a,"b",c'              || [["a", "b", "c"]]
        "quoted last field"                | 'a,b,"c"'              || [["a", "b", "c"]]
        "all fields quoted"                | '"a","b","c"'          || [["a", "b", "c"]]
        "empty quoted middle field"        | 'a,"",b'               || [["a", "", "b"]]
        "empty quoted last field"          | 'a,""'                 || [["a", ""]]
        "quoted field before CR"           | '"a"\rb'               || [["a"], ["b"]]
        "quoted field after a line break"  | 'a\r"b"'               || [["a"], ["b"]]
        "quoted spaces are kept"           | '" a "'                || [[" a "]]
        "multiline field then more lines"  | '"a\rb",c\rd'          || [["a\rb", "c"], ["d"]]
    }

    def "escaped quotes: #description"() {
        expect:
        driver.parse(input) == expected

        where:
        description                        | input                  || expected
        "at the beginning of the field"    | '"""a"'                || [['"a']]
        "in the middle of the field"       | '"a""b"'               || [['a"b']]
        "at the end of the field"          | '"a"""'                || [['a"']]
        "the whole field"                  | '""""'                 || [['"']]
        "two in a row"                     | '"a""""b"'             || [['a""b']]
        "several in one field"             | '"""a""b"""'           || [['"a"b"']]
        "before a comma inside quotes"     | '"a"",b"'              || [['a",b']]
        "before a CR inside quotes"        | '"a""\rb"'             || [['a"\rb']]
        "in the first field"               | '"a""b",c'             || [['a"b', "c"]]
        "in the middle field"              | 'x,"a""b",y'           || [["x", 'a"b', "y"]]
        "in the last field"                | 'x,"a""b"'             || [["x", 'a"b']]
        "at the beginning, before a comma" | '"""",b'               || [['"', "b"]]
        "at the end, before a CR"          | '"a"""\rb'             || [['a"'], ["b"]]
        "in several fields and lines"      | '"a""",""""\r"""b"'    || [['a"', '"'], ['"b']]
    }

    def "rejects what a strict format does not allow: #description"() {
        when:
        driver.parse(input)

        then:
        thrown(UnexpectedCharacterException)

        where:
        description                                  | input
        "quote inside an unquoted field"             | 'a"b'
        "quote at the end of an unquoted field"      | 'a"'
        "quote after a leading space"                | ' "a"'
        "text after the closing quote"               | '"a"b'
        "space after the closing quote"              | '"a" '
        "LF after the closing quote"                 | '"a"\nb'
        "text after an empty quoted field"           | '""a'
        "quoted field never closed"                  | '"a'
        "only an opening quote"                      | '"'
        "quoted field never closed, with a comma"    | '"a,b'
        "escaped quote, then the end of file"        | '"a""'
        "error on a later line"                      | 'a,b\rc"d'
    }

    def "either parses or throws UnexpectedCharacterException, on every input of up to 6 characters"() {
        given:
        def alphabet = ["a", ",", '"', "\r", "\n"]
        def inputs = [""]
        def longest = [""]
        6.times {
            longest = longest.collectMany { s -> alphabet.collect { s + it } }
            inputs += longest
        }

        when:
        inputs.each {
            try {
                driver.parse(it)
            } catch (UnexpectedCharacterException ignored) {
            }
        }

        then:
        noExceptionThrown()
        inputs.size() == 1 + 5 + 25 + 125 + 625 + 3125 + 15625
    }

    def "every field survives quoting"() {
        given:
        def values = ["", "a", " ", ",", '"', '""', "\n", "\r", "\r\n", 'a"b', '"a', 'a"', "a,b", '","', "a\rb"]
        def rows = []
        values.each { v -> rows << [v] }
        values.each { v -> values.each { w -> rows << [v, w] } }
        values.each { v -> rows << ["x", v, "y"] }

        when:
        def input = rows.collect { row -> row.collect { '"' + it.replace('"', '""') + '"' }.join(",") }.join("\r")

        then:
        driver.parse(input) == rows
    }
}
