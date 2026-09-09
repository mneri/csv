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

package me.mneri.csv

import me.mneri.csv.deserializer.Deserializer
import me.mneri.csv.io.internal.RandomAccessStream
import me.mneri.csv.line.internal.InternalRecycledLine
import me.mneri.csv.parser.internal.LineParser
import spock.lang.Specification
import spock.lang.Unroll

class CsvReaderTest extends Specification {
    def stream = new RandomAccessStream(Mock(Reader), 65_536);
    def line = new InternalRecycledLine(stream)
    def parser = Mock(LineParser)
    def deserializer = Stub(Deserializer)
    def reader = new CsvReader(parser, line, deserializer)

    def "hasNext() and next() return values in order"() {
        given:
        parser.next(line) >>> [true, false]
        deserializer.deserialize(line) >> ["apple", "banana", "cherry"]

        when:
        def hasFirst = reader.hasNext()

        then:
        hasFirst

        when:
        def value = reader.next()

        then:
        value == ["apple", "banana", "cherry"]

        when:
        def hasSecond = reader.hasNext()

        then:
        !hasSecond
    }

    def "hasNext() twice in a row does not advance to the next element"() {
        given:
        parser.next(line) >>> [true, false]
        deserializer.deserialize(line) >> ["apple", "banana", "cherry"]

        when:
        def hasFirst = reader.hasNext()

        then:
        hasFirst

        when:
        def hasAgain = reader.hasNext()

        then:
        hasAgain

        when:
        def value = reader.next()

        then:
        value == ["apple", "banana", "cherry"]
    }

    def "next() returns an element even when hasNext() was never called first"() {
        given:
        parser.next(line) >>> [true, false]
        deserializer.deserialize(line) >> ["apple", "banana", "cherry"]

        when:
        def value = reader.next()

        then:
        value == ["apple", "banana", "cherry"]

        when:
        def hasNext = reader.hasNext()

        then:
        !hasNext
    }

    def "next() throws NoSuchElementException when there are no more elements"() {
        given:
        parser.next(line) >> false

        when:
        reader.next()

        then:
        thrown(NoSuchElementException)
    }

    @Unroll
    def "#label throws IllegalStateException when the reader is closed"() {
        given:
        reader.close()

        when:
        action(reader)

        then:
        thrown(IllegalStateException)

        where:
        label       | action
        "hasNext()" | { r -> r.hasNext() }
        "next()"    | { r -> r.next() }
    }
}
