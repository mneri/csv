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

package me.mneri.csv.line.internal

import me.mneri.csv.exception.NoSuchFieldException
import me.mneri.csv.io.internal.RandomAccessStream
import spock.lang.Specification
import spock.lang.Unroll

class InternalRecycledLineTest extends Specification {
    private static RandomAccessStream streamOver(String content) {
        int capacity = Integer.highestOneBit(Math.max(64, content.length()) - 1) << 2
        new RandomAccessStream(new StringReader(content), capacity)
    }

    def "rejects a null stream"() {
        when:
        new InternalRecycledLine(null)

        then:
        thrown(IllegalArgumentException)
    }

    def "reports zero fields before anything is recorded"() {
        given:
        def line = new InternalRecycledLine(streamOver(""))

        expect:
        line.getFieldCount() == 0
    }

    def "counts one entry per startField()/endField() pair"() {
        given:
        def stream = streamOver("aabbcc")
        def line = new InternalRecycledLine(stream)

        when:
        line.startField(0); line.endField(2)
        line.startField(2); line.endField(4)
        line.startField(4); line.endField(6)

        then:
        line.getFieldCount() == 3
        line.getString(0) == "aa"
        line.getString(1) == "bb"
        line.getString(2) == "cc"
    }

    def "reset() clears fields and exclusions, and previously valid indices become invalid"() {
        given:
        def stream = streamOver("hello")
        def line = new InternalRecycledLine(stream)
        line.startField(0)
        line.endField(5)

        when:
        line.reset()

        then:
        line.getFieldCount() == 0

        when:
        line.getString(0)

        then:
        thrown(NoSuchFieldException)
    }

    def "throws NoSuchFieldException for an index at or beyond the field count"() {
        given:
        def stream = streamOver("x")
        def line = new InternalRecycledLine(stream)

        when:
        line.startField(0)
        line.endField(1)
        line.getString(1)

        then:
        thrown(NoSuchFieldException)
    }

    def "getString() reads a clean field straight from the stream"() {
        given:
        def stream = streamOver("hello,world")
        def line = new InternalRecycledLine(stream)
        line.startField(0)
        line.endField(5)
        line.startField(6)
        line.endField(11)

        expect:
        line.getString(0) == "hello"
        line.getString(1) == "world"
    }

    def "getString() returns null for a zero-length field"() {
        given:
        def stream = streamOver("ab")
        def line = new InternalRecycledLine(stream)
        line.startField(1)
        line.endField(1)

        expect:
        line.getString(0) == null
    }

    def "getString() collapses a single doubled-quote pair marked dirty"() {
        given:
        def stream = streamOver('a""b')
        def line = new InternalRecycledLine(stream)
        line.startField(0)
        line.dirty(1)
        line.endField(4)

        expect:
        line.getString(0) == 'a"b'
    }

    def "getString() collapses multiple doubled-quote pairs in the same field"() {
        given:
        def stream = streamOver('a""b""c')
        def line = new InternalRecycledLine(stream)
        line.startField(0)
        line.dirty(1)
        line.dirty(4)
        line.endField(7)

        expect:
        line.getString(0) == 'a"b"c'
    }

    def "getString() correctly separates dirty and clean fields on the same line"() {
        given:
        def stream = streamOver('clean,a""b')
        def line = new InternalRecycledLine(stream)
        line.startField(0)
        line.endField(5)
        line.startField(6)
        line.dirty(7)
        line.endField(10)

        expect:
        line.getString(0) == "clean"
        line.getString(1) == 'a"b'
    }

    def "getCharArray() copies a clean field and returns its length"() {
        given:
        def stream = streamOver("hello")
        def line = new InternalRecycledLine(stream)
        def dest = new char[5]
        line.startField(0)
        line.endField(5)

        when:
        int copied = line.getCharArray(0, dest, 0)

        then:
        copied == 5
        new String(dest) == "hello"
    }

    def "getCharArray() writes at the requested destination offset"() {
        given:
        def stream = streamOver("hi")
        def line = new InternalRecycledLine(stream)
        def dest = "___".toCharArray()
        line.startField(0)
        line.endField(2)

        when:
        int copied = line.getCharArray(0, dest, 1)

        then:
        copied == 2
        new String(dest) == "_hi"
    }


    def "getCharArray() collapses a dirty field and returns the post-exclusion length"() {
        given:
        def stream = streamOver('a""b')
        def line = new InternalRecycledLine(stream)
        def dest = new char[3]
        line.startField(0)
        line.dirty(1)
        line.endField(4)

        when:
        int copied = line.getCharArray(0, dest, 0)

        then:
        copied == 3
        new String(dest, 0, copied) == 'a"b'
    }

    def "getBigDecimal() parses a clean field"() {
        given:
        def stream = streamOver("12.50")
        def line = new InternalRecycledLine(stream)
        line.startField(0)
        line.endField(5)

        expect:
        line.getBigDecimal(0) == new BigDecimal("12.50")
    }

    def "getBigDecimal() parses a dirty field after collapsing excluded characters"() {
        given:
        def stream = streamOver('1"2.5')
        def line = new InternalRecycledLine(stream)
        line.startField(0)
        line.dirty(1)
        line.endField(5)

        expect:
        line.getBigDecimal(0) == new BigDecimal('12.5')
        line.getBigDecimal(0) == new BigDecimal('12.5')
    }


    @Unroll
    def "getBigInteger() honors an explicit radix (#radix)"() {
        given:
        def stream = streamOver(text)
        def line = new InternalRecycledLine(stream)
        line.startField(0); line.endField(text.length())

        expect:
        line.getBigInteger(0, radix) == expected

        where:
        text  | radix | expected
        "255" | 10    | new BigInteger("255")
        "ff"  | 16    | new BigInteger("255")
        "11"  | 2     | new BigInteger("3")
    }

    def "getBigInteger() defaults to radix 10"() {
        given:
        def stream = streamOver("42")
        def line = new InternalRecycledLine(stream)
        line.startField(0); line.endField(2)

        expect:
        line.getBigInteger(0) == new BigInteger("42")
    }

    def "getBigInteger() parses a dirty field with a non-default radix"() {
        given:
        def stream = streamOver('f"f')
        def line = new InternalRecycledLine(stream)
        line.startField(0)
        line.dirty(1)
        line.endField(3)

        expect:
        line.getBigInteger(0, 16) == new BigInteger("ff", 16)
    }


    def "getDouble() parses a clean field"() {
        given:
        def stream = streamOver("3.14159")
        def line = new InternalRecycledLine(stream)
        line.startField(0); line.endField(7)

        expect:
        line.getDouble(0, -1.0d) == 3.14159d
    }

    def "getDouble() returns the default for an empty field, without touching the stream's parser"() {
        given:
        def stream = streamOver("x")
        def line = new InternalRecycledLine(stream)
        line.startField(0); line.endField(0)

        expect:
        line.getDouble(0, -1.0d) == -1.0d
    }

    def "getDouble() parses a dirty field after collapsing it"() {
        given:
        def stream = streamOver('1"2.0')
        def line = new InternalRecycledLine(stream)
        line.startField(0)
        line.dirty(1)
        line.endField(5)

        expect:
        line.getDouble(0, -1.0d) == 12.0d
    }

    def "getFloat() parses a clean field"() {
        given:
        def stream = streamOver("2.5")
        def line = new InternalRecycledLine(stream)
        line.startField(0); line.endField(3)

        expect:
        line.getFloat(0, -1.0f) == 2.5f
    }

    def "getFloat() returns the default for an empty field"() {
        given:
        def stream = streamOver("x")
        def line = new InternalRecycledLine(stream)
        line.startField(0); line.endField(0)

        expect:
        line.getFloat(0, -1.0f) == -1.0f
    }

    def "getInteger() defaults to radix 10 and returns the default on an empty field"() {
        given:
        def stream = streamOver("42")
        def line = new InternalRecycledLine(stream)
        line.startField(0); line.endField(2)
        line.startField(2); line.endField(2) // empty

        expect:
        line.getInteger(0, -1) == 42
        line.getInteger(1, -1) == -1
    }

    def "getInteger() honors an explicit radix"() {
        given:
        def stream = streamOver("ff")
        def line = new InternalRecycledLine(stream)
        line.startField(0); line.endField(2)

        expect:
        line.getInteger(0, 16, -1) == 255
    }

    def "getLong() defaults to radix 10 and returns the default on an empty field"() {
        given:
        def stream = streamOver("123456789012")
        def line = new InternalRecycledLine(stream)
        line.startField(0); line.endField(12)
        line.startField(12); line.endField(12)

        expect:
        line.getLong(0, -1L) == 123456789012L
        line.getLong(1, -1L) == -1L
    }

    def "getShort() honors an explicit radix"() {
        given:
        def stream = streamOver("ff")
        def line = new InternalRecycledLine(stream)
        line.startField(0); line.endField(2)

        expect:
        line.getShort(0, 16, (short) -1) == (short) 255
    }


    def "getUnsignedInteger() parses a value beyond Integer.MAX_VALUE"() {
        given:
        def stream = streamOver("4294967295")
        def line = new InternalRecycledLine(stream)
        line.startField(0); line.endField(10)

        expect:
        line.getUnsignedInteger(0, -1) == -1 // bit pattern of 4294967295 as a signed int
        Integer.toUnsignedLong(line.getUnsignedInteger(0, -1)) == 4294967295L
    }

    def "getUnsignedLong() returns the default on an empty field"() {
        given:
        def stream = streamOver("x")
        def line = new InternalRecycledLine(stream)
        line.startField(0); line.endField(0)

        expect:
        line.getUnsignedLong(0, -1L) == -1L
    }

    @Unroll
    def "getBoolean() parses '#text' as #expected"() {
        given:
        def stream = streamOver(text)
        def line = new InternalRecycledLine(stream)
        line.startField(0); line.endField(text.length())

        expect:
        line.getBoolean(0, false) == expected

        where:
        text    | expected
        "true"  | true
        "TRUE"  | true
        "false" | false
        "nope"  | false
    }


    def "getBoolean() returns the default on an empty field"() {
        given:
        def stream = streamOver("x")
        def line = new InternalRecycledLine(stream)
        line.startField(0); line.endField(0)

        expect:
        line.getBoolean(0, true)
    }

    def "grows the coordinates array past its initial 256-field capacity"() {
        given:
        int fieldCountTarget = 300
        def content = "x" * fieldCountTarget
        def stream = streamOver(content)
        def line = new InternalRecycledLine(stream)

        when:
        (0..<fieldCountTarget).each { i -> line.startField(i); line.endField(i + 1) }

        then:
        line.getFieldCount() == fieldCountTarget
        line.getString(0) == "x"
        line.getString(fieldCountTarget - 1) == "x"
    }
}
