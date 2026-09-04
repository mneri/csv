package me.mneri.csv.io.internal;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.BufferOverflowException;

import static org.junit.jupiter.api.Assertions.*;

class BufferedRandomAccessReaderTest {
    @Test
    void constructorRejectsNullReader() {
        assertThrows(IllegalArgumentException.class, () -> {
            try (BufferedRandomAccessReader ignored = new BufferedRandomAccessReader(null, 1024)) {
            }
        });
    }

    @Test
    void constructorRejectsZeroCapacity() {
        assertThrows(IllegalArgumentException.class, () -> {
            try (BufferedRandomAccessReader ignored = new BufferedRandomAccessReader(new StringReader(""), 0)) {
            }
        });
    }

    @Test
    void constructorRejectsNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> {
            try (BufferedRandomAccessReader ignored = new BufferedRandomAccessReader(new StringReader(""), -1)) {
            }
        });
    }

    @Test
    void constructorRejectsNonPowerOfTwoCapacity() {
        assertThrows(IllegalArgumentException.class, () -> {
            try (BufferedRandomAccessReader ignored = new BufferedRandomAccessReader(new StringReader(""), 1000)) {
            }
        });
    }

    @Test
    void constructorAcceptsPowerOfTwoCapacity() throws IOException {
        try (BufferedRandomAccessReader ignored = new BufferedRandomAccessReader(new StringReader(""), 1024)) {
        }
    }

    @Test
    void getCharReadsSequentially() throws IOException {
        String s = "Hello, world!";
        try (BufferedRandomAccessReader reader = new BufferedRandomAccessReader(new StringReader(s), 16)) {
            for (int i = 0; i < s.length(); i++) {
                assertEquals(s.charAt(i), reader.getChar(i));
            }
        }
    }

    @Test
    void getCharSupportsRandomAccess() throws IOException {
        String s = "Hello, world!";
        try (BufferedRandomAccessReader reader = new BufferedRandomAccessReader(new StringReader(s), 16)) {
            assertEquals(s.charAt(5), reader.getChar(5));
            assertEquals(s.charAt(1), reader.getChar(1));
            assertEquals(s.charAt(7), reader.getChar(7));
        }
    }

    @Test
    void getCharAtEndOfStreamReturnsMinusOne() throws IOException {
        String s = "Hello, world!";
        try (BufferedRandomAccessReader reader = new BufferedRandomAccessReader(new StringReader(s), 16)) {
            assertEquals(-1, reader.getChar(s.length()));
        }
    }

    @Test
    void getCharPastEndOfStreamReturnsMinusOne() throws IOException {
        String s = "Hello, world!";
        try (BufferedRandomAccessReader reader = new BufferedRandomAccessReader(new StringReader(s), 16)) {
            assertEquals(-1, reader.getChar(s.length() + 1000));
        }
    }

    @Test
    void getStringReturnsRequestedRange() throws IOException {
        String s = "Hello, world!";
        try (BufferedRandomAccessReader reader = new BufferedRandomAccessReader(new StringReader(s), 16)) {
            assertEquals(s.substring(2, 5), reader.getString(2, 5));
        }
    }

    @Test
    void getStringWithEqualStartAndEndReturnsEmptyString() throws IOException {
        String s = "Hello, world!";
        try (BufferedRandomAccessReader reader = new BufferedRandomAccessReader(new StringReader(s), 16)) {
            assertEquals("", reader.getString(3, 3));
        }
    }

    @Test
    void getStringPastEndOfStreamThrowsIndexOutOfBounds() throws IOException {
        String s = "Hello, world!";
        try (BufferedRandomAccessReader reader = new BufferedRandomAccessReader(new StringReader(s), 16)) {
            assertThrows(IndexOutOfBoundsException.class, () -> {
                String ignored = reader.getString(0, s.length() + 1);
            });
        }
    }

    @Test
    void getCharArrayCopiesRequestedRange() throws IOException {
        String s = "Hello, world!";
        try (BufferedRandomAccessReader reader = new BufferedRandomAccessReader(new StringReader(s), 16)) {
            char[] dest = new char[5];
            reader.getCharArray(dest, 0, 0, 5);
            assertEquals(s.substring(0, 5), new String(dest));
        }
    }

    @Test
    void getCharArrayRespectsDestPosOffset() throws IOException {
        String s = "Hello, world!";
        try (BufferedRandomAccessReader reader = new BufferedRandomAccessReader(new StringReader(s), 16)) {
            char[] dest = new char[10];
            reader.getCharArray(dest, 2, 6, 11);
            assertEquals(s.substring(6, 11), new String(dest, 2, 5));
        }
    }

    @Test
    void getCharArrayPastEndOfStreamThrowsIndexOutOfBounds() throws IOException {
        String s = "Hello, world!";
        try (BufferedRandomAccessReader reader = new BufferedRandomAccessReader(new StringReader(s), 16)) {
            char[] dest = new char[5];
            assertThrows(IndexOutOfBoundsException.class, () -> reader.getCharArray(dest, 0, 0, s.length() + 1));
        }
    }

    @Test
    void compactAllowsReadingNextWindowAfterDiscardingOldOne() throws IOException {
        String s = "Hello, world!";
        try (BufferedRandomAccessReader reader = new BufferedRandomAccessReader(new StringReader(s), 16)) {
            assertEquals(s.substring(0, 4), reader.getString(0, 4));
            reader.compact(4);
            assertEquals(s.substring(4, 8), reader.getString(4, 8));
        }
    }

    @Test
    void compactAheadOfUnreadDataSkipsForwardInStream() throws IOException {
        String s = "Hello, world!";
        // compact() to a position not yet read from the underlying stream.
        try (BufferedRandomAccessReader reader = new BufferedRandomAccessReader(new StringReader(s), 16)) {
            reader.compact(5);
            assertEquals(s.substring(5, 8), reader.getString(5, 8));
        }
    }

    @Test
    void compactToAlreadyDiscardedPositionThrowsIndexOutOfBounds() throws IOException {
        String s = "Hello, world!";
        try (BufferedRandomAccessReader reader = new BufferedRandomAccessReader(new StringReader(s), 16)) {
            reader.compact(6);
            assertThrows(IndexOutOfBoundsException.class, () -> reader.compact(2));
        }
    }

    @Test
    void slidingWindowOverStreamLongerThanCapacityReproducesOriginalContent() throws IOException {
        String data = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        int stride = 4;
        StringBuilder rebuilt = new StringBuilder();
        try (BufferedRandomAccessReader reader = new BufferedRandomAccessReader(new StringReader(data), 8)) {
            for (int i = 0; i < data.length(); i += stride) {
                int end = Math.min(i + stride, data.length());
                rebuilt.append(reader.getString(i, end));
                reader.compact(end);
            }
        }
        assertEquals(data, rebuilt.toString());
    }

    @Test
    void requestingSpanWiderThanCapacityThrowsBufferOverflow() {
        String s = "Hello, world!";
        try (BufferedRandomAccessReader reader = new BufferedRandomAccessReader(new StringReader(s), 8)) {
            assertThrows(BufferOverflowException.class, () -> reader.getString(0, s.length()));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void regularCompactionKeepsWideStreamWithinSmallCapacity() throws IOException {
        String s = "1234567890";
        try (BufferedRandomAccessReader reader = new BufferedRandomAccessReader(new StringReader(s), 4)) {
            assertEquals(s.substring(0, 4), reader.getString(0, 4));
            reader.compact(4);
            assertEquals(s.substring(4, 8), reader.getString(4, 8));
            reader.compact(8);
            assertEquals(s.substring(8, 10), reader.getString(8, 10));
        }
    }

    @Test
    void operationsAfterCloseThrowIOException() throws IOException {
        BufferedRandomAccessReader reader = new BufferedRandomAccessReader(new StringReader("abc"), 8);
        reader.close();
        assertThrows(IOException.class, () -> reader.getString(0, 1));
    }

    @Test
    void closeIsIdempotent() throws IOException {
        BufferedRandomAccessReader r = new BufferedRandomAccessReader(new StringReader("abc"), 8);
        r.close();
        assertDoesNotThrow(r::close);
    }

    @Test
    void closeDelegatesToUnderlyingReader() throws IOException {
        boolean[] closed = {false};
        Reader delegate = new StringReader("abc") {
            @Override
            public void close() {
                closed[0] = true;
            }
        };
        try (BufferedRandomAccessReader r = new BufferedRandomAccessReader(delegate, 8)) {
            assertEquals('a', r.getChar(0));
        }
        assertTrue(closed[0]);
    }

    @Test
    void ioExceptionFromUnderlyingReaderPropagates() {
        Reader failing = new Reader() {
            @Override
            public int read(char[] buf, int off, int len) throws IOException {
                throw new IOException("Boom");
            }

            @Override
            public void close() {
            }
        };
        try (BufferedRandomAccessReader reader = new BufferedRandomAccessReader(failing, 8)) {
            IOException ex = assertThrows(IOException.class, () -> reader.getChar(0));
            assertEquals("Boom", ex.getMessage());
        } catch (IOException e) {
            fail(e);
        }
    }

}