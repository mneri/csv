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

import jdk.incubator.vector.VectorSpecies;

import java.io.IOException;
import java.io.Reader;
import java.nio.BufferOverflowException;

/**
 * A high-throughput, character reader that provides absolute random access over a strictly sequential underlying
 * {@link Reader}.
 * <p>
 * To provide random access, this reader uses a finite memory buffer. Because the buffer capacity is bounded, the client
 * must continuously advance the accessible data window by calling {@link #compact(long)}. This signals that older
 * characters are no longer required, discarding them to free capacity for new data from the stream. Attempting to read
 * an index prior to the compaction point will result in an unspecified behaviour.
 * <p>
 * <b>Example:</b>
 * <pre>{@code
 *     // Read a chunk of characters from absolute stream positions 0 to 99
 *     String token = reader.getString(50, 100);
 *
 *     // Signal that indices up to 99 will never be accessed again,
 *     // allowing the reader to free memory and consume further data from
 *     // the stream.
 *     reader.compact(100);
 * }</pre>
 * <p>
 * To maximise raw I/O throughput, this class is inherently thread-unsafe. Exposing a single instance to concurrent
 * threads will result in immediate state tearing of the cursor offsets and data corruption.
 */
public interface RandomAccessStream extends AutoCloseable {
    /**
     * Close the character stream.
     *
     * @throws IOException If an I/O error occurs.
     */
    @Override
    void close() throws IOException;

    /**
     * Compact the backing buffer, moving the start of the window to the specified position.
     * <p>
     * By calling this method, the client is telling that characters preceding the specified position are no longer
     * required and can be discarded.
     * <p>
     * In this implementation, the client is responsible for ensuring that characters preceding the specified position
     * are no longer accessed. If the client subsequently accesses a character that has been freed by this method (for
     * example, by calling {@link #getChar(long)}), the behavior is unspecified.
     *
     * @param pos The absolute position.
     * @throws IOException               If an I/O error occurs.
     * @throws IndexOutOfBoundsException If the requested position is out of the buffered window.
     */
    void compact(long pos) throws IOException;

    /**
     * Return a bitmask with bits set in the positions of the specified character.
     * <p>
     * This operation leverages the CPU's SIMD registers. The vector species defines the size of the registers, and the
     * number of consecutive characters to check.
     *
     * @param pos     The offset in the character source.
     * @param species The species of the CPU's SIMD registers.
     * @param c       The character to find.
     * @return A bitmask.
     */
    long getBitmask(long pos, VectorSpecies<Short> species, char c);

    /**
     * Return a bitmask with bits set in the positions of the specified characters.
     * <p>
     * This operation leverages the CPU's SIMD registers. The vector species defines the size of the registers, and the
     * number of consecutive characters to check.
     *
     * @param pos     The offset in the character source.
     * @param species The species of the CPU's SIMD registers.
     * @param c1      A character to find.
     * @param c2      A character to find.
     * @return A bitmask.
     */
    long getBitmask(long pos, VectorSpecies<Short> species, char c1, char c2);

    /**
     * Return a bitmask with bits set in the positions of the specified characters.
     * <p>
     * This operation leverages the CPU's SIMD registers. The vector species defines the size of the registers, and the
     * number of consecutive characters to check.
     *
     * @param pos     The offset in the character source.
     * @param species The species of the CPU's SIMD registers.
     * @param c1      A character to find.
     * @param c2      A character to find.
     * @param c3      A character to find.
     * @return A bitmask.
     */
    long getBitmask(long pos, VectorSpecies<Short> species, char c1, char c2, char c3);

    /**
     * Return a bitmask with bits set in the positions of the specified characters.
     * <p>
     * This operation leverages the CPU's SIMD registers. The vector species defines the size of the registers, and the
     * number of consecutive characters to check.
     *
     * @param pos     The offset in the character source.
     * @param species The species of the CPU's SIMD registers.
     * @param c1      A character to find.
     * @param c2      A character to find.
     * @param c3      A character to find.
     * @param c4      A character to find.
     * @return A bitmask.
     */
    long getBitmask(long pos, VectorSpecies<Short> species, char c1, char c2, char c3, char c4);

    /**
     * Return a bitmask with bits set in the positions of the specified characters.
     * <p>
     * This operation leverages the CPU's SIMD registers. The vector species defines the size of the registers, and the
     * number of consecutive characters to check.
     *
     * @param pos     The offset in the character source.
     * @param species The species of the CPU's SIMD registers.
     * @param c1      A character to find.
     * @param c2      A character to find.
     * @param c3      A character to find.
     * @param c4      A character to find.
     * @param c5      A character to find.
     * @return A bitmask.
     */
    long getBitmask(long pos, VectorSpecies<Short> species, char c1, char c2, char c3, char c4, char c5);

    /**
     * Get the character at the specified position.
     * <p>
     * For performance reasons, behavior is unspecified if {@code pos} precedes a position that has been freed by a
     * previous call to {@link #compact(long)}. The client must ensure that characters discarded by {@code compact()}
     * are never accessed again.
     *
     * @param pos The absolute position.
     * @return The character.
     * @throws IOException               If an I/O error occurs.
     * @throws IndexOutOfBoundsException If the requested position is out of the buffered window.
     * @throws BufferOverflowException   If the requested position exceeds the internal buffer limits.
     */
    int getChar(long pos) throws IOException;

    /**
     * Get the characters between the two specified indexes as a character array.
     *
     * @param dest    The array to copy the characters to.
     * @param destPos The starting position in the destination array.
     * @param start   The starting position (inclusive).
     * @param end     The end position (exclusive).
     * @return The number of characters copied in the destination array.
     * @throws IOException               If an I/O error occurs.
     * @throws IndexOutOfBoundsException If the requested positions are out of the buffered window.
     * @throws BufferOverflowException   If the requested positions exceeds the internal buffer limits.
     */
    int getCharArray(char[] dest, int destPos, long start, long end) throws IOException;

    /**
     * Get the characters between the two specified indexes as string.
     *
     * @param start The starting position (inclusive).
     * @param end   The end position (exclusive).
     * @return The string.
     * @throws IOException               If an I/O error occurs.
     * @throws IndexOutOfBoundsException If the requested position is out of the buffered window.
     * @throws BufferOverflowException   If the requested position exceeds the internal buffer limits.
     */
    String getString(long start, long end) throws IOException;
}
