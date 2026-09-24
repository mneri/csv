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

import spock.lang.Shared
import spock.lang.Specification

class MsExcelFormatTest extends Specification {
    @Shared
    def comma = new FormatDriver(MsExcelFormat.provider(Locale.US)) // Decimal separator '.', delimiter ','

    @Shared
    def semicolon = new FormatDriver(MsExcelFormat.provider(Locale.ITALY)) // Decimal separator ',', delimiter ';'

    private static semi(value) {
        value instanceof String ? value.replace(',', ';') : value.collect { semi(it) }
    }

    def "empty input and blank lines: #description"() {
        expect:
        comma.parse(input) == expected
        semicolon.parse(semi(input)) == semi(expected)

        where:
        description                      | input        || expected
        "empty input"                    | ""           || []
        "LF only"                        | "\n"         || [[""]]
        "CR LF only"                     | "\r\n"       || [[""]]
        "CR only"                        | "\r"         || [[""]]
        "two LF"                         | "\n\n"       || [[""], [""]]
        "two CR"                         | "\r\r"       || [[""], [""]]
        "LF then CR are two line breaks" | "\n\r"       || [[""], [""]]
        "blank line between lines"       | "a\n\nb"     || [["a"], [""], ["b"]]
        "blank line with CR LF"          | "a\r\n\r\nb" || [["a"], [""], ["b"]]
        "blank line at the end"          | "a\n\n"      || [["a"], [""]]
    }

    def "unquoted fields: #description"() {
        expect:
        comma.parse(input) == expected
        semicolon.parse(semi(input)) == semi(expected)

        where:
        description                     | input                      || expected
        "one character"                 | "a"                        || [["a"]]
        "one field"                     | "abc"                      || [["abc"]]
        "two fields"                    | "a,b"                      || [["a", "b"]]
        "three fields"                  | "a,b,c"                    || [["a", "b", "c"]]
        "only a comma"                  | ","                        || [["", ""]]
        "only two commas"               | ",,"                       || [["", "", ""]]
        "empty last field"              | "a,"                       || [["a", ""]]
        "empty first field"             | ",a"                       || [["", "a"]]
        "empty middle field"            | "a,,b"                     || [["a", "", "b"]]
        "spaces are kept"               | " a , b "                  || [[" a ", " b "]]
        "tab is ordinary"               | "a\tb,c"                   || [["a\tb", "c"]]
        "punctuation below comma"       | "!#\$%&'()*+"              || [["!#\$%&'()*+"]]
        "control characters"            | "\u0000\u0001\u000B\u000C" || [["\u0000\u0001\u000B\u000C"]]
        "non-ASCII characters"          | "é,中文,😀"                 || [["é", "中文", "😀"]]
        "U+FFFF is not the end of file" | "a\uFFFFb,c"               || [["a\uFFFFb", "c"]]
        "long field"                    | "x" * 10_000               || [["x" * 10_000]]
    }

    def "line breaks: #description"() {
        expect:
        comma.parse(input) == expected
        semicolon.parse(semi(input)) == semi(expected)

        where:
        description                      | input           || expected
        "LF between lines"               | "a,b\nc,d"      || [["a", "b"], ["c", "d"]]
        "CR LF between lines"            | "a,b\r\nc,d"    || [["a", "b"], ["c", "d"]]
        "CR between lines"               | "a,b\rc,d"      || [["a", "b"], ["c", "d"]]
        "LF at the end"                  | "a,b\n"         || [["a", "b"]]
        "CR LF at the end"               | "a,b\r\n"       || [["a", "b"]]
        "CR at the end"                  | "a,b\r"         || [["a", "b"]]
        "mixed line breaks"              | "a\nb\r\nc\rd"  || [["a"], ["b"], ["c"], ["d"]]
        "LF CR is two line breaks"       | "a\n\rb"        || [["a"], [""], ["b"]]
        "different number of fields"     | "a,b,c\nd\ne,f" || [["a", "b", "c"], ["d"], ["e", "f"]]
        "empty last field before LF"     | "a,\nb"         || [["a", ""], ["b"]]
        "empty last field before CR LF"  | "a,\r\nb"       || [["a", ""], ["b"]]
        "empty last field before CR"     | "a,\rb"         || [["a", ""], ["b"]]
        "comma right after a line break" | "a\n,b"         || [["a"], ["", "b"]]
    }

    def "quoted fields: #description"() {
        expect:
        comma.parse(input) == expected
        semicolon.parse(semi(input)) == semi(expected)

        where:
        description                       | input         || expected
        "one quoted field"                | '"a"'         || [["a"]]
        "empty quoted field"              | '""'          || [[""]]
        "quoted field with comma"         | '"a,b"'       || [["a,b"]]
        "quoted field with LF"            | '"a\nb"'      || [["a\nb"]]
        "quoted field with CR LF"         | '"a\r\nb"'    || [["a\r\nb"]]
        "quoted field with CR"            | '"a\rb"'      || [["a\rb"]]
        "quoted field with only a LF"     | '"\n"'        || [["\n"]]
        "quoted first field"              | '"a",b,c'     || [["a", "b", "c"]]
        "quoted middle field"             | 'a,"b",c'     || [["a", "b", "c"]]
        "quoted last field"               | 'a,b,"c"'     || [["a", "b", "c"]]
        "all fields quoted"               | '"a","b","c"' || [["a", "b", "c"]]
        "empty quoted middle field"       | 'a,"",b'      || [["a", "", "b"]]
        "empty quoted last field"         | 'a,""'        || [["a", ""]]
        "quoted field before LF"          | '"a"\nb'      || [["a"], ["b"]]
        "quoted field before CR LF"       | '"a"\r\nb'    || [["a"], ["b"]]
        "quoted field before CR"          | '"a"\rb'      || [["a"], ["b"]]
        "quoted field after a line break" | 'a\n"b"'      || [["a"], ["b"]]
        "quoted spaces are kept"          | '" a "'       || [[" a "]]
        "multiline field then more lines" | '"a\nb",c\nd' || [["a\nb", "c"], ["d"]]
    }

    def "escaped quotes: #description"() {
        expect:
        comma.parse(input) == expected
        semicolon.parse(semi(input)) == semi(expected)

        where:
        description                        | input               || expected
        "at the beginning of the field"    | '"""a"'             || [['"a']]
        "in the middle of the field"       | '"a""b"'            || [['a"b']]
        "at the end of the field"          | '"a"""'             || [['a"']]
        "the whole field"                  | '""""'              || [['"']]
        "two in a row"                     | '"a""""b"'          || [['a""b']]
        "several in one field"             | '"""a""b"""'        || [['"a"b"']]
        "before a comma inside quotes"     | '"a"",b"'           || [['a",b']]
        "before a LF inside quotes"        | '"a""\nb"'          || [['a"\nb']]
        "in the first field"               | '"a""b",c'          || [['a"b', "c"]]
        "in the middle field"              | 'x,"a""b",y'        || [["x", 'a"b', "y"]]
        "in the last field"                | 'x,"a""b"'          || [["x", 'a"b']]
        "at the beginning, before a comma" | '"""",b'            || [['"', "b"]]
        "at the end, before a LF"          | '"a"""\nb'          || [['a"'], ["b"]]
        "in several fields and lines"      | '"a""",""""\n"""b"' || [['a"', '"'], ['"b']]
    }

    def "quotes inside an unquoted field are ordinary characters: #description"() {
        expect:
        comma.parse(input) == expected
        semicolon.parse(semi(input)) == semi(expected)

        where:
        description                              | input           || expected
        "in the middle"                          | 'a"b'           || [['a"b']]
        "at the end"                             | 'a"'            || [['a"']]
        "two in a row"                           | 'a""b'          || [['a""b']]
        "documented example: b\"b\"b"            | 'aaa,b"b"b,ccc' || [["aaa", 'b"b"b', "ccc"]]
        "documented example: y\"y"               | 'xxx,y"y,zzz'   || [["xxx", 'y"y', "zzz"]]
        "after a leading space"                  | ' "a"'          || [[' "a"']]
        "the comma after a quote ends the field" | 'a",b'          || [['a"', "b"]]
    }

    def "text after the closing quote belongs to the field: #description"() {
        expect:
        comma.parse(input) == expected
        semicolon.parse(semi(input)) == semi(expected)

        where:
        description                        | input            || expected
        "documented example: \"bb\"b"      | 'aaa,"bb"b,ccc'  || [["aaa", "bbb", "ccc"]]
        "documented example: \"y\"yy\""    | 'xxx,"y"yy",zzz' || [["xxx", 'yyy"', "zzz"]]
        "a space after the closing quote"  | '"a" '           || [["a "]]
        "text after an empty quoted field" | '""a'            || [["a"]]
        "text then a line break"           | '"a"b\nc'        || [["ab"], ["c"]]
        "text in a middle field"           | 'x,"a"b,y'       || [["x", "ab", "y"]]
        "a comma after the text"           | '"a"b,'          || [["ab", ""]]
    }

    def "a quoted field that is never closed ends at the end of file: #description"() {
        expect:
        comma.parse(input) == expected
        semicolon.parse(semi(input)) == semi(expected)

        where:
        description                        | input          || expected
        "documented example"               | 'aaa,bbb,"ccc' || [["aaa", "bbb", "ccc"]]
        "only an opening quote"            | '"'            || [[""]]
        "opening quote as last field"      | 'a,"'          || [["a", ""]]
        "unclosed field with a comma"      | '"a,b'         || [["a,b"]]
        "unclosed field with a LF"         | '"a\nb'        || [["a\nb"]]
        "unclosed field after other lines" | 'a\n"b'        || [["a"], ["b"]]
        "escaped quote then end of file"   | '"a""'         || [['a"']]
        "escaped quote at the beginning"   | '"""'          || [['"']]
    }

    def "the delimiter depends on the decimal separator of the locale: #locale"() {
        expect:
        MsExcelFormat.provider(locale).provide().delimiter() == delimiter

        where:
        locale         || delimiter
        Locale.US      || (int) ','
        Locale.UK      || (int) ','
        Locale.ITALY   || (int) ';'
        Locale.GERMANY || (int) ';'
        Locale.FRANCE  || (int) ';'
    }

    def "the other delimiter is an ordinary character: #description"() {
        expect:
        driver.parse(input) == expected

        where:
        description                                    | driver    | input           || expected
        "semicolon with the comma delimiter"           | comma     | "a;b,c"         || [["a;b", "c"]]
        "quoted semicolon with the comma delimiter"    | comma     | '"a;b",c'       || [["a;b", "c"]]
        "comma with the semicolon delimiter"           | semicolon | "a,b;c"         || [["a,b", "c"]]
        "decimal numbers with the semicolon delimiter" | semicolon | "1,5;2,25\n3,0" || [["1,5", "2,25"], ["3,0"]]
        "documented example with the Italian locale"   | semicolon | 'aaa;bbb;"ccc'  || [["aaa", "bbb", "ccc"]]
    }

    def "never throws, on every input of up to 5 characters made of a, comma, semicolon, quote, CR and LF"() {
        given:
        def alphabet = ["a", ",", ";", '"', "\r", "\n"]
        def inputs = [""]
        def longest = [""]
        5.times {
            longest = longest.collectMany { s -> alphabet.collect { s + it } }
            inputs += longest
        }

        when:
        inputs.each {
            comma.parse(it)
            semicolon.parse(it)
        }

        then:
        noExceptionThrown()
        inputs.size() == 1 + 6 + 36 + 216 + 1296 + 7776
    }

    def "every field survives quoting, with the #delimiter delimiter and #name line breaks"() {
        given:
        def values = ["", "a", " ", ",", ";", '"', '""', "\n", "\r", "\r\n", 'a"b', '"a', 'a"', "a,b", "a;b", '","',
                      "a\nb"]
        def rows = []
        values.each { v -> rows << [v] }
        values.each { v -> values.each { w -> rows << [v, w] } }
        values.each { v -> rows << ["x", v, "y"] }

        when:
        def input = rows.collect { row -> row.collect { '"' + it.replace('"', '""') + '"' }.join(delimiter) }
                .join(lineBreak)

        then:
        driver.parse(input) == rows

        where:
        driver    | delimiter | name    | lineBreak
        comma     | ","       | "LF"    | "\n"
        comma     | ","       | "CR LF" | "\r\n"
        comma     | ","       | "CR"    | "\r"
        semicolon | ";"       | "LF"    | "\n"
        semicolon | ";"       | "CR LF" | "\r\n"
        semicolon | ";"       | "CR"    | "\r"
    }
}
