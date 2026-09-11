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
import me.mneri.csv.format.Format
import me.mneri.csv.io.internal.RandomAccessStream
import me.mneri.csv.line.internal.InternalRecycledLine
import spock.lang.Specification

import static me.mneri.csv.format.Format.*

class VectorLineParserTest extends Specification {
    // For performance reasons, RandomAccessStream and InternalRecycledLine are final classes, and it's not possible to
    // mock or spy final classes. Tests make use of these two classes so they don't look properly isolated, but we
    // really can't do better.

    def stream = new RandomAccessStream(new StringReader(('0'..'9').join()), 65_536);
    def line = new InternalRecycledLine(stream)

    def provider = { format } as Format.Provider
    def format = Mock(Format) { base() >> 0 }
    def parser = new VectorLineParser(provider, stream)

    def "immediately ends the stream"() {
        given:
        format.bitmask((int) _, (char[]) _, (int) _) >> 0x01L
        format.consumeSlow((int) _, (int) _) >> STP

        when:
        def hasLine = parser.next(line)

        then:
        !hasLine
    }

    def "starts a field, then ends the field and the line at the end of the stream"() {
        given:
        format.bitmask((int) _, (char[]) _, (int) _) >> 0x0C_01L
        format.consumeSlow((int) _, (int) _) >>> [SFH, EFH | ELH, STP]

        when:
        def hasLine = parser.next(line)

        then:
        hasLine
        line.getFieldCount() == 1
        line.getString(0) == "0123456789"

        when:
        def hasAgain = parser.next(line)

        then:
        !hasAgain
    }

    def "starts a field, then ends a field, then ends the line"() {
        given:
        format.bitmask((int) _, (char[]) _, (int) _) >> 0x0FL
        format.consumeSlow((int) _, (int) _) >>> [SFH, EFH, ELH, STP]

        when:
        def hasLine = parser.next(line)

        then:
        hasLine
        line.getFieldCount() == 1
        line.getString(0) == "0"

        when:
        def hasAgain = parser.next(line)

        then:
        !hasAgain
    }

    def "starts and end a field at the same position, then ends the line"() {
        given:
        format.bitmask((int) _, (char[]) _, (int) _) >> 0x07L
        format.consumeSlow((int) _, (int) _) >>> [SFH | EFH, ELH, STP]

        when:
        def hasLine = parser.next(line)

        then:
        hasLine
        line.getFieldCount() == 1
        line.getString(0) == null

        when:
        def hasAgain = parser.next(line)

        then:
        !hasAgain
    }

    def "starts a field, then advances, then finds a dirty character, then ends a field, then ends the line"() {
        given:
        format.bitmask((int) _, (char[]) _, (int) _) >> 0x3DL
        format.consumeSlow((int) _, (int) _) >>> [SFH, RMB, EFH, ELH, STP]

        when:
        def hasLine = parser.next(line)

        then:
        hasLine
        line.getFieldCount() == 1
        line.getString(0) == "02" // "012" but "1" is removed by RMB

        when:
        def hasAgain = parser.next(line)

        then:
        !hasAgain
    }

    def "starts a field, then ends the field and replays, then starts a field, then ends a field, then ends the line"() {
        given:
        format.bitmask((int) _, (char[]) _, (int) _) >> 0x3FL
        format.consumeSlow((int) _, (int) _) >>> [SFH, EFH | RPL, SFH, EFH, ELH, STP]

        when:
        def hasLine = parser.next(line)

        then:
        hasLine
        line.getFieldCount() == 2
        line.getString(0) == "0"
        line.getString(1) == "1"

        when:
        def hasAgain = parser.next(line)

        then:
        !hasAgain
    }

    def "finds an unexpected character"() {
        given:
        format.bitmask((int) _, (char[]) _, (int) _) >> 0x01L
        format.consumeSlow((int) _, (int) _) >>> [ERH]

        when:
        parser.next(line)

        then:
        thrown(UnexpectedCharacterException)
    }
}
