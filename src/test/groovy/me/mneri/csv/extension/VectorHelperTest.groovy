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

package me.mneri.csv.extension

import spock.lang.Specification

class VectorHelperTest extends Specification {
    def "#description"() {
        given:
        def cb = ("x" * 64).toCharArray()
        positions.each { cb[it] = c }

        expect:
        VectorHelper.bitmaskUleEqEq(cb, 0, '\r' as char, '"' as char, ',' as char) == mask

        where:
        description                      | c                 | positions || mask
        "no match"                       | 'x' as char       | []        || 0L
        "a character equal to ule"       | '\r' as char      | [0]       || 1L
        "a character below ule"          | '\n' as char      | [1]       || 2L
        "a character equal to eq1"       | '"' as char       | [2]       || 4L
        "a character equal to eq2"       | ',' as char       | [63]      || Long.MIN_VALUE
        "several matches"                | ',' as char       | [0, 31]   || (1L << 31) + 1
        "every character matches"        | ',' as char       | (0..63)   || -1L
        "ule is compared as unsigned"    | '\uFFFF' as char  | [0]       || 0L
    }

    def "reads the 64 characters starting at the offset"() {
        given:
        def cb = ("," + "x" * 64 + ",").toCharArray()

        expect:
        VectorHelper.bitmaskUleEqEq(cb, 1, '\r' as char, '"' as char, ',' as char) == 0L
    }
}
