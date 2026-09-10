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

package me.mneri.csv.deserializer

import me.mneri.csv.line.RecycledLine
import spock.lang.Specification

class StringArrayDeserializerTest extends Specification {
    private final StringArrayDeserializer des = new StringArrayDeserializer()

    def "deserialize() deserializes all fields into a list of strings"() {
        given:
        def line = Mock(RecycledLine) {
            getFieldCount() >> 3
            getString(0) >> "Alice"
            getString(1) >> "42"
            getString(2) >> "London"
        }

        when:
        def result = des.deserialize(line)

        then:
        result == ["Alice", "42", "London"] as String[]
    }

    def "deserialize() returns an empty list when line has no fields"() {
        given:
        def line = Mock(RecycledLine) {
            getFieldCount() >> 0
        }

        when:
        def result = des.deserialize(line)

        then:
        result == [] as String[]
    }
}
