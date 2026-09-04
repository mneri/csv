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

package me.mneri.csv.io.internal;

import java.io.IOException;
import java.io.Reader;
import java.nio.BufferOverflowException;

public class RandomAccessCharStream implements RandomAccessStream {
    // The class is architected to heavily exploit HotSpot JIT compiler, specifically targeting array range check
    // elimination and aggressive method inlining.
    //
    // We restrict the buffer capacity to a power of two, so we can calculate array indices using a simple bitwise AND
    // mask. The JIT compiler should this mask and should remove the array bounds checks from the machine code.
    //
    // Hot methods like getChar() are kept tiny so the JIT compiler can inline them easily. We strip out all checks,
    // exception throwing, and file I/O, pushing that heavy logic into separate "cold" methods like getChar2() that
    // only run when needed.

    private static final int STATE_OPEN = 0;
    private static final int STATE_CLOSED = 1;

    private static final int READ_SIZE = 8_192;

    private char[] cb;
    private final int mask;
    private long first; // Absolute index of the first buffered character
    private long last; // Absolute index (exclusive) of the last buffered character
    private long offset; // Offset of cb[0] (i.e. if cb[0] contains 100th character on the stream, offset is -100)
    private int limit; // Relative index of the last buffered character
    private Reader in;
    private int state = STATE_OPEN;

    /**
     * Create a new reader.
     *
     * @param in       The underlying character stream to read from.
     * @param capacity The buffer capacity, in characters. Must be a positive power of two. This bounds the maximum span
     *                 between the oldest and newest positions that can be live at once; choosing it too small will
     *                 cause {@link BufferOverflowException} at runtime rather than at construction time.
     */
    public RandomAccessCharStream(Reader in, int capacity) {
        if (in == null) {
            throw new IllegalArgumentException("Reader cannot be null");
        }
        if (capacity <= 0 || (capacity & (capacity - 1)) != 0) {
            throw new IllegalArgumentException("Capacity must be a positive power of two: " + capacity);
        }
        this.in = in;
        this.cb = new char[capacity];
        this.mask = capacity - 1;
    }

    @Override
    public void close() throws IOException {
        if (state == STATE_CLOSED) {
            return;
        }
        try {
            in.close();
        } finally {
            limit = 0;
            first = last = offset = -1;
            in = null;
            cb = null;
            state = STATE_CLOSED;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param pos {@inheritDoc}
     * @return {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    @Override
    public int getChar(long pos) throws IOException { // Bytecode size: 33 (OpenJDK 26)
        // This method is split in two to minimise the fast-path bytecode size and enable aggressive inlining by the
        // C2 compiler. State checks, buffer loads and exception throwing are delegated to getChar2().

        // Re-enabling the pos >= first check below would provide stronger guarantees by detecting accesses to positions
        // freed by compact() and correctly throwing an IndexOutOfBoundsException. The check has been removed for
        // performance reasons, and the result is left unspecified; callers must ensure that freed positions are never
        // accessed.
        if (/*pos >= first &&*/ pos < last) { // Hot path
            // If the reader is closed, accessing cb throws a NullPointerException; rough, but OK. Avoiding the check on
            // state is giving another nice boost in performance.
            return cb[((int) (pos + offset)) & mask]; // Mask to avoid Java's boundary check; NOT a circular buffer!
        }
        // Cold path: executed when the client requests characters that have not yet been read from the underlying
        // reader; roughly once every READ_SIZE invocations of this method, if the client reads characters sequentially.
        return getChar2(pos);
    }

    private int getChar2(long pos) throws IOException {
        if (state == STATE_CLOSED) {
            readerIsClosedException();
        }
        if (pos < first) {
            indexOutOfBoundsException(pos);
        }
        // At this point: pos >= last
        read(pos);
        if (pos >= last) { // If the requested position is still >= last we've reached EOF
            return -1;
        }
        return cb[((int) (pos + offset)) & mask];
    }

    /**
     * {@inheritDoc}
     *
     * @param dest    {@inheritDoc}
     * @param destPos {@inheritDoc}
     * @param start   {@inheritDoc}
     * @param end     {@inheritDoc}
     * @return
     * @throws IOException {@inheritDoc}
     */
    @Override
    public int getCharArray(char[] dest, int destPos, long start, long end) throws IOException {
        // This method is split in two to minimise the fast-path bytecode size and enable aggressive inlining by the
        // C2 compiler. State checks, buffer loads and exception throwing is delegated to getCharArray2().

        // If state == STATE_CLOSED will throw a NullPointerException; rough, but OK. Avoiding the check on state is
        // giving a nice boost in performance.
        if (start >= first && end <= last) {
            System.arraycopy(cb, (int) (start + offset), dest, destPos, (int) (end - start));
            return (int) (end - start);
        }
        return getCharArray2(dest, destPos, start, end); // Cold path: executed when the requested chars were never read
    }

    private int getCharArray2(char[] dest, int destPos, long start, long end) throws IOException {
        if (state == STATE_CLOSED) {
            readerIsClosedException();
        }
        if (start < first) {
            indexOutOfBoundsException(start);
        }
        // At this point: end > last
        read(end - 1);
        System.arraycopy(cb, (int) (start + offset), dest, destPos, (int) (end - start));
        return (int) (end - start);
    }

    /**
     * {@inheritDoc}
     *
     * @param start {@inheritDoc}
     * @param end   {@inheritDoc}
     * @return {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    @Override
    public String getString(long start, long end) throws IOException { // Bytecode size: 54 (OpenJDK 26)
        // This method is split in two to minimise the fast-path bytecode size and enable aggressive inlining by the
        // C2 compiler. State checks, buffer loads and exception throwing is delegated to getString2().

        if (start == end) {
            return null;
        }

        // If state == STATE_CLOSED will throw a NullPointerException; rough, but OK. Avoiding the check on state is
        // giving a nice boost in performance.
        if (start >= first && end <= last) { // Hot path
            return new String(cb, (int) (start + offset), (int) (end - start));
        }
        return getString2(start, end); // Cold path: executed when the requested chars were never read
    }

    private String getString2(long start, long end) throws IOException {
        if (state == STATE_CLOSED) {
            readerIsClosedException();
        }
        if (start < first) {
            indexOutOfBoundsException(start);
        }
        // At this point: end > last
        read(end - 1);
        if (end > last) {
            indexOutOfBoundsException(end);
        }
        return new String(cb, (int) (start + offset), (int) (end - start));
    }

    /**
     * {@inheritDoc}
     *
     * @param pos {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    @Override
    public void compact(long pos) throws IOException { // Bytecode size: 32 (OpenJDK 26)
        // Minimises fast-path bytecode size to enable aggressive method inlining by C2. Delegates stream checks, buffer
        // loads, and exception throwing to the cold path on compact2().

        if (pos >= first && pos <= last) { // Hot path
            // Compaction drops old characters we no longer need to make room for new ones. This process is lazy: we do
            // not free memory when compact() is called, but only when we run out of space (see read()).
            first = pos;
        } else {
            compact2(pos); // Cold path: executed only if the client compacts characters that were never read
        }
    }

    private void compact2(long pos) throws IOException {
        if (state == STATE_CLOSED) {
            readerIsClosedException();
        }
        if (pos < first) {
            indexOutOfBoundsException(pos);
        }
        // Here, pos > last. The client is trying to compact() to a position that hasn't been read yet. This is very
        // expensive, as we need to consume characters from the underlying stream.
        skipTo(pos - last); // Might skip less if EOF happens prematurely
    }

    private void skipTo(long pos) throws IOException {
        if (pos < last) {
            return;
        }

        long toSkip = pos - last;
        long remaining = toSkip;
        while (remaining > 0L) {
            long chunk = Math.min(remaining, READ_SIZE); // Skip the stream in chunks of READ_SIZE.
            long skipped = in.skip(chunk); // skip() returns 0 on stream not ready or EOF (unlike read() which returns 0 and -1)
            if (skipped > 0L) {
                remaining -= skipped;
            } else {
                // Since in.skip() returns 0 on stream not ready or EOF we have no way of knowing which one happened,
                // so we force a read() and if it's -1 we assume the stream has ended.
                if (in.read() == -1) {
                    break;
                }
                remaining--;
            }
        }
        long skipped = toSkip - remaining;

        first = last = last + skipped;
        offset = -first;
        limit = 0;
    }

    private void read(long pos) throws IOException {
        while (pos >= last) { // While the requested position is greater than the last position read from disk
            int space = cb.length - limit;
            if (space < READ_SIZE) {
                // This is not a circular buffer (ring buffer). Because we maintain a strictly linear contiguous memory
                // layout, we must linearly shift the unconsumed data to the left (towards index 0) to coalesce free
                // space at the tail of the array.
                space = shift();
                // If a shift yields zero free space, the buffer is entirely saturated with unconsumed data.
                if (space == 0) {
                    bufferOverflowException();
                }
            }
            // We attempt to maximise the I/O read payload to minimise system calls, capping it at either our predefined
            // READ_SIZE chunking threshold or the available contiguous space.
            int toRead = Math.min(READ_SIZE, space);
            int read = in.read(cb, limit, toRead);
            if (read > 0) {
                limit += read;
                last += read;
            }
            if (read == -1) {
                break;
            }
        }
    }

    private int shift() { // Bytecode size: 8 (OpenJDK 26)
        int start = (int) (first + offset);
        if (start > 0) {
            int length = limit - start;
            System.arraycopy(cb, start, cb, 0, length);
            offset -= start;
            limit = length;
        }
        return cb.length - limit;
    }

    private void bufferOverflowException() {
        throw new BufferOverflowException();
    }

    private void indexOutOfBoundsException(long pos) {
        throw new IndexOutOfBoundsException("" + pos); // Java 8 compatibility
    }

    private void readerIsClosedException() throws IOException {
        throw new IOException("Stream closed");
    }

    @Override
    public char[] array(long start, long end) throws IOException {
        if (end <= last) {
            return cb;
        }
        return array2(start, end);
    }

    private char[] array2(long start, long end) throws IOException {
        if (state == STATE_CLOSED) {
            readerIsClosedException();
        }
        if (start < first) {
            indexOutOfBoundsException(start);
        }
        read(end - 1);
        return cb;
    }

    @Override
    public int index(long pos) {
        return (int) (pos + offset);
    }
}
