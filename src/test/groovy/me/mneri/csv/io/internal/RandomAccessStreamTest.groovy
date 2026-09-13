package me.mneri.csv.io.internal

import spock.lang.Specification
import spock.lang.Unroll

import java.nio.BufferOverflowException

class RandomAccessStreamTest extends Specification {
    def "rejects a null Reader"() {
        when:
        new RandomAccessStream(null, 16)

        then:
        thrown(IllegalArgumentException)
    }


    @Unroll
    def "rejects a capacity that is not a positive power of two (#capacity)"() {
        when:
        new RandomAccessStream(new StringReader(""), capacity)

        then:
        thrown(IllegalArgumentException)

        where:
        capacity << [0, -1, -16, 3, 5, 100, 1000]
    }

    @Unroll
    def "accepts a valid power-of-two capacity (#capacity)"() {
        when:
        def stream = new RandomAccessStream(new StringReader(""), capacity)

        then:
        noExceptionThrown()

        cleanup:
        stream?.close()

        where:
        capacity << [1, 2, 4, 16, 1024, 8192]
    }

    def "close() is idempotent"() {
        given:
        def stream = new RandomAccessStream(new StringReader("abc"), 4)

        when:
        stream.close()
        stream.close()

        then:
        noExceptionThrown()
    }

    def "reads characters sequentially when the whole stream fits in the buffer"() {
        given:
        def stream = new RandomAccessStream(new StringReader("abcdefgh"), 16)

        expect:
        (0..<8).collect { stream.getChar(it) as char } == "abcdefgh".toList()

        cleanup:
        stream.close()
    }

    def "assembles a value that requires several underlying read() calls to fully arrive"() {
        given:
        // A real StringReader always hands back everything available up to the requested length in one call, so it
        // can't exercise read()'s internal accumulation loop on its own. This mock dribbles 2 characters per call to
        // force multiple real reads before the requested range is fully buffered with a capacity generous enough that
        // no compaction is needed, isolating the multi-read loop from the eviction logic.
        char[] source = "abcdefgh".toCharArray()
        int position = 0
        def reader = Mock(Reader) {
            read(_, _, _) >> { char[] buf, int off, int len ->
                if (position >= source.length) {
                    return -1
                }
                int chunk = Math.min(2, Math.min(len, source.length - position))
                System.arraycopy(source, position, buf, off, chunk)
                position += chunk
                chunk
            }
        }
        def stream = new RandomAccessStream(reader, 16)

        expect:
        stream.getString(0, 8) == "abcdefgh"

        cleanup:
        stream.close()
    }

    def "getChar() returns -1 past the end of the stream"() {
        given:
        def stream = new RandomAccessStream(new StringReader("ab"), 4)

        expect:
        stream.getChar(0) == ('a' as char) as int
        stream.getChar(1) == ('b' as char) as int
        stream.getChar(2) == -1

        cleanup:
        stream.close()
    }

    def "getChar() throws IOException once the stream is closed"() {
        given:
        def stream = new RandomAccessStream(new StringReader("abc"), 4)
        stream.close()

        when:
        stream.getChar(0)

        then:
        thrown(IOException)
    }

    def "getChars() copies the requested range into the destination array when characters are already in the buffer"() {
        given:
        def stream = new RandomAccessStream(new StringReader("hello world"), 16)
        def dest = new char[5]
        stream.getChar(10)

        when:
        stream.getChars(dest, 0, 6, 5)

        then:
        new String(dest) == "world"

        cleanup:
        stream.close()
    }

    def "getChars() copies the requested range into the destination array when characters are not yet in the buffer"() {
        given:
        def stream = new RandomAccessStream(new StringReader("hello world"), 16)
        def dest = new char[5]

        when:
        stream.getChars(dest, 0, 6, 5)

        then:
        new String(dest) == "world"

        cleanup:
        stream.close()
    }

    def "getChars() writes at the requested destination offset"() {
        given:
        def stream = new RandomAccessStream(new StringReader("hello"), 8)
        def dest = new char[7]
        Arrays.fill(dest, '_' as char)

        when:
        stream.getChars(dest, 2, 0, 5)

        then:
        new String(dest) == "__hello"

        cleanup:
        stream.close()
    }

    def "getChars() copies the requested range into the destination array when data is not yet buffered"() {
        given:
        def stream = new RandomAccessStream(new StringReader("abcdefghij"), 16)

        when:
        def dest = new char[4]
        stream.getChars(dest, 0, 6, 4)

        then:
        new String(dest) == "ghij"

        cleanup:
        stream.close()
    }

    def "getChars() throws IndexOutOfBoundsException when requesting past end of stream"() {
        given:
        def stream = new RandomAccessStream(new StringReader("abc"), 4)
        def dest = new char[10]

        when:
        stream.getChars(dest, 0, 0, 10)

        then:
        thrown(IndexOutOfBoundsException)

        cleanup:
        stream.close()
    }

    def "getChars() throws IndexOutOfBoundsException for a position already freed by compact()"() {
        given:
        def stream = new RandomAccessStream(new StringReader("abcdefgh"), 8)
        stream.compact(5)
        def dest = new char[3]

        when:
        stream.getChars(dest, 0, 0, 3)

        then:
        thrown(IndexOutOfBoundsException)

        cleanup:
        stream.close()
    }

    def "getString() returns null for a zero length"() {
        given:
        def reader = Mock(Reader)
        def stream = new RandomAccessStream(reader, 4)

        expect:
        stream.getString(0, 0) == null

        cleanup:
        stream.close()
    }

    def "getString() reads a value already in the buffer"() {
        given:
        def stream = new RandomAccessStream(new StringReader("hello,world"), 16)
        stream.getChar(10) // force the whole string into the buffer first

        expect:
        stream.getString(0, 5) == "hello"
        stream.getString(6, 5) == "world"

        cleanup:
        stream.close()
    }

    def "getString() reads a value not yet in the buffer"() {
        given:
        def stream = new RandomAccessStream(new StringReader("hello,world"), 16)

        expect:
        stream.getString(6, 5) == "world"

        cleanup:
        stream.close()
    }

    def "getString() throws IndexOutOfBoundsException for a position already freed by compact()"() {
        given:
        def stream = new RandomAccessStream(new StringReader("abcdefgh"), 8)
        stream.compact(5)

        when:
        stream.getString(0, 3)

        then:
        thrown(IndexOutOfBoundsException)

        cleanup:
        stream.close()
    }

    def "getString() throws BufferOverflowException when a requested range can never fit in the buffer"() {
        given:
        def stream = new RandomAccessStream(new StringReader("abcdefghijklmnop"), 4)

        when:
        stream.getString(0, 10)

        then:
        thrown(BufferOverflowException)

        cleanup:
        stream.close()
    }

    def "compact() to a position beyond what has been read skips ahead in the underlying stream"() {
        given:
        def stream = new RandomAccessStream(new StringReader("abcdefghij"), 16)

        when:
        stream.compact(5)

        then:
        stream.getChar(5) == ('f' as char) as int

        cleanup:
        stream.close()
    }

    def "compact() past the end of a short stream does not hang, and getChar() reports EOF afterwards"() {
        given:
        def stream = new RandomAccessStream(new StringReader("abc"), 8)

        when:
        stream.compact(100)

        then:
        noExceptionThrown()
        stream.getChar(3) == -1

        cleanup:
        stream.close()
    }

    def "parseBigDecimal() parses a value already in the buffer"() {
        given:
        def stream = new RandomAccessStream(new StringReader("12.50,ignored"), 16)
        stream.getChar(12)

        expect:
        stream.parseBigDecimal(0, 5) == new BigDecimal("12.50")

        cleanup:
        stream.close()
    }

    def "parseBigDecimal() parses a value not yet in the buffer"() {
        given:
        def stream = new RandomAccessStream(new StringReader("12.50,ignored"), 16)

        expect:
        stream.parseBigDecimal(0, 5) == new BigDecimal("12.50")

        cleanup:
        stream.close()
    }

    def "parseBigDecimal() returns null for length == 0"() {
        given:
        def stream = new RandomAccessStream(new StringReader(""), 16)

        expect:
        stream.parseBigDecimal(0, 0) == null
    }

    def "parseBigInteger() parses a value already in the buffer"() {
        given:
        def stream = new RandomAccessStream(new StringReader("12345,ignored"), 16)
        stream.getChar(12)

        expect:
        stream.parseBigInteger(0, 5, 10) == new BigInteger("12345")

        cleanup:
        stream.close()
    }

    def "parseBigInteger() parses a value not yet in the buffer"() {
        given:
        def stream = new RandomAccessStream(new StringReader("12345,ignored"), 16)

        expect:
        stream.parseBigInteger(0, 5, 10) == new BigInteger("12345")

        cleanup:
        stream.close()
    }

    def "parseBigInteger() returns null for length == 0"() {
        given:
        def stream = new RandomAccessStream(new StringReader(""), 16)

        expect:
        stream.parseBigInteger(0, 0, 10) == null
    }

    @Unroll
    def "parseBigInteger() honors the given radix (#radix)"() {
        given:
        def stream = new RandomAccessStream(new StringReader(text), 16)

        expect:
        stream.parseBigInteger(0, text.length(), radix) == expected

        cleanup:
        stream.close()

        where:
        text  | radix | expected
        "255" | 10    | new BigInteger("255")
        "ff"  | 16    | new BigInteger("255")
        "11"  | 2     | new BigInteger("3")
    }

    def "parseDouble() parses a value already in the buffer"() {
        given:
        def stream = new RandomAccessStream(new StringReader("3.14159"), 16)
        stream.getChar(6)

        expect:
        stream.parseDouble(0, 7, 0.0d) == 3.14159d

        cleanup:
        stream.close()
    }

    def "parseDouble() parses a value not yet in the buffer"() {
        given:
        def stream = new RandomAccessStream(new StringReader("3.14159"), 16)

        expect:
        stream.parseDouble(0, 7, 0.0d) == 3.14159d

        cleanup:
        stream.close()
    }

    def "parseDouble() returns the default value for a zero-length field"() {
        given:
        def stream = new RandomAccessStream(new StringReader(""), 4)

        expect:
        stream.parseDouble(0, 0, -1.0d) == -1.0d

        cleanup:
        stream.close()
    }

    def "parseFloat() parses a value already in the buffer"() {
        given:
        def stream = new RandomAccessStream(new StringReader("3.14159"), 16)
        stream.getChar(6)

        expect:
        stream.parseFloat(0, 7, 0.0f) == 3.14159f

        cleanup:
        stream.close()
    }

    def "parseFloat() parses a value not yet in the buffer"() {
        given:
        def stream = new RandomAccessStream(new StringReader("3.14159"), 16)

        expect:
        stream.parseFloat(0, 7, 0.0f) == 3.14159f

        cleanup:
        stream.close()
    }

    def "parseFloat() returns the default value for a zero-length field"() {
        given:
        def stream = new RandomAccessStream(new StringReader(""), 4)

        expect:
        stream.parseFloat(0, 0, -1.0f) == -1.0f

        cleanup:
        stream.close()
    }
}
