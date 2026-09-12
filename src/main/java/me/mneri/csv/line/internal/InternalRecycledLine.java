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

package me.mneri.csv.line.internal;

import me.mneri.csv.exception.NoSuchFieldException;
import me.mneri.csv.io.internal.RandomAccessStream;
import me.mneri.csv.line.RecycledLine;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * Internal implementation of the {@link RecycledLine} interface. <i>This class is internal and is not meant to be used
 * by clients.</i>
 * <p>
 * {@code InternalRecycledLine}, {@code RandomAccessStream} and {@code LineParser} implementations are closely related,
 * and together they compose the parsing and data extraction behaviour. As the stream is processed, the line parser
 * populates this object by recording positional markers via calls to {@link #startField(long)},
 * {@link #endField(long)}, and {@link #dirty(long)}. Rather than eagerly allocating or copying strings,
 * {@code InternalRecycledLine} only tracks the fields' coordinates. When a field value is subsequently requested by the
 * client (for example, via {@link #getString(int)}), {@code InternalRecycledLine} uses the saved markers to read and
 * assemble the data directly from the underlying stream on demand. The same instance of {@code RandomAccessStream} is
 * shared between the {@code LineParser} and {@code InternalRecycledLine}, so what the parser dictates can be
 * effectively reconstructed by {@code InternalRecycledLine}.
 *
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public final class InternalRecycledLine implements RecycledLine {
    private final RandomAccessStream stream;

    // Memory Layout & Indexing Strategy:
    //
    // Each parsed field is encoded using 3 consecutive entries in the 'coordinates' array:
    //   1. Start position (inclusive)
    //   2. End position (exclusive)
    //   3. Exclusions pointer (-1 if clean, or an index into the 'exclusions' array if dirty characters exist)
    //
    // coordinates array:
    //   ... +--------------------+--------------------+--------------------+ ...
    //       | start pos (long)   |   end pos (long)   |   exclusions ptr   |
    //       +--------------------+--------------------+--------------------+
    //                                                          |
    //                                                          +------+
    // exclusions array:                                               V
    //   ... +----------------------------------------------+--------------------+--------------------+ ...
    //                                                      | exclusion pos      | exclusion pos      |
    //       +----------------------------------------------+--------------------+--------------------+

    private long[] coordinates = new long[768];
    private int coordinatesSize;

    private long[] exclusions = new long[32];
    private int exclusionsSize;

    private char[] staging = new char[512];

    /**
     * Construct a new {@code InternalRecycledLine}.
     *
     * @param stream The character stream.
     */
    public InternalRecycledLine(RandomAccessStream stream) {
        if (stream == null) {
            throw new IllegalArgumentException("Reader cannot be null");
        }
        this.stream = stream;
    }

    /**
     * Mark the start a field at the specified position in the character stream.
     *
     * @param pos The position.
     */
    public void startField(long pos) {
        if (coordinatesSize + 3 > coordinates.length) {
            growCoordinates();
        }
        coordinates[coordinatesSize] = pos;
        coordinates[coordinatesSize + 2] = -1; // Initially marked as not dirty
    }

    /**
     * Mark the end of the current field at the specified position in the character stream. The client must have called
     * {@link #startField(long)} before.
     * <p>
     * No object is actually created at this stage.
     *
     * @param pos The position.
     */
    public void endField(long pos) {
        coordinates[coordinatesSize + 1] = pos;
        coordinatesSize += 3;
    }

    /**
     * Mark the character at the specified position as dirty.
     * <p>
     * Dirty characters are stripped off, and are not part of the output of methods such as {@link #getString(int)}.
     * This is important when dealing with escaped sequences inside a qualified field.
     *
     * @param pos The position.
     */
    public void dirty(long pos) {
        if (coordinates[coordinatesSize + 2] == -1) {
            coordinates[coordinatesSize + 2] = exclusionsSize;
        }
        if (exclusionsSize >= exclusions.length) {
            growExclusions();
        }
        exclusions[exclusionsSize++] = pos;
    }

    /**
     * Reset the state of this line. All the fields created with calls to {@link #startField(long)} and
     * {@link #endField(long)}, and all the dirty characters marked with {@link #dirty(long)} are removed.
     */
    public void reset() {
        coordinatesSize = 0;
        exclusionsSize = 0;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public int getFieldCount() {
        return coordinatesSize / 3;
    }

    /**
     * {@inheritDoc}
     *
     * @param n {@inheritDoc}
     * @return {@inheritDoc}
     * @throws {@inheritDoc}
     */
    @Override
    public String getString(int n) throws IOException {
        if (n >= getFieldCount()) {
            noSuchFieldException(n);
        }
        int i = n * 3;
        if (coordinates[i + 2] == -1) { // If the field is marked NOT dirty
            return stream.getString(coordinates[i], coordinates[i + 1]);
        }
        return getDirtyString(n);
    }

    private String getDirtyString(int n) throws IOException {
        int length = getDirtyCharArray(n, staging, 0);
        return new String(staging, 0, length);
    }

    /**
     * {@inheritDoc}
     *
     * @param n {@inheritDoc}
     * @return {@inheritDoc}
     * @throws {@inheritDoc}
     */
    @Override
    public BigDecimal getBigDecimal(int n) throws IOException {
        int length = getCharArray(n, staging, 0);
        return new BigDecimal(staging, 0, length);
    }

    /**
     * {@inheritDoc}
     *
     * @param n {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public BigInteger getBigInteger(int n) throws IOException {
        return getBigInteger(n, 10);
    }

    /**
     * {@inheritDoc}
     *
     * @param n {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public BigInteger getBigInteger(int n, int radix) throws IOException {
        String value = getString(n);
        return value == null ? null : new BigInteger(value, radix);
    }

    /**
     * {@inheritDoc}
     *
     * @param n {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Boolean getBoolean(int n) throws IOException {
        String value = getString(n);
        return value == null ? null : Boolean.parseBoolean(value);
    }

    @Override
    public int getCharArray(int n, char[] dest, int destPos) throws IOException {
        if (n >= getFieldCount()) {
            noSuchFieldException(n);
        }
        int i = n * 3;
        if (coordinates[i + 2] == -1) { // If the field is marked NOT dirty
            long start = coordinates[i];
            long end = coordinates[i + 1];
            stream.getCharArray(dest, destPos, start, end);
            return (int) (end - start);
        }
        return getDirtyCharArray(n, dest, destPos);
    }

    private int getDirtyCharArray(int n, char[] dest, int destPos) throws IOException {
        int i = n * 3;

        long start = coordinates[i];
        long end = coordinates[i + 1];
        int copied = 0;

        int j = (int) coordinates[i + 2];
        while (j < exclusionsSize && exclusions[j] < end) {
            long stop = exclusions[j++];
            stream.getCharArray(dest, destPos + copied, start, stop);
            copied += (int) (stop - start);
            start = stop + 1;
        }

        stream.getCharArray(dest, destPos + copied, start, end);
        copied += (int) (end - start);

        return copied;
    }

    /**
     * {@inheritDoc}
     *
     * @param n {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Double getDouble(int n) throws IOException {
        String value = getString(n);
        return value == null ? null : Double.parseDouble(value);
    }

    /**
     * {@inheritDoc}
     *
     * @param n {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Float getFloat(int n) throws IOException {
        String value = getString(n);
        return value == null ? null : Float.parseFloat(value);
    }

    /**
     * {@inheritDoc}
     *
     * @param n {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Integer getInteger(int n) throws IOException {
        return getInteger(n, 10);
    }

    /**
     * {@inheritDoc}
     *
     * @param n     {@inheritDoc}
     * @param radix {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Integer getInteger(int n, int radix) throws IOException {
        String value = getString(n);
        return value == null ? null : Integer.parseInt(value, radix);
    }

    /**
     * {@inheritDoc}
     *
     * @param n {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Long getLong(int n) throws IOException {
        return getLong(n, 10);
    }

    /**
     * {@inheritDoc}
     *
     * @param n     {@inheritDoc}
     * @param radix {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Long getLong(int n, int radix) throws IOException {
        String value = getString(n);
        return value == null ? null : Long.parseLong(value, radix);
    }

    /**
     * {@inheritDoc}
     *
     * @param n {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Short getShort(int n) throws IOException {
        return getShort(n, 10);
    }

    /**
     * {@inheritDoc}
     *
     * @param n     {@inheritDoc}
     * @param radix {@inheritDoc}
     * @return {@inheritDoc}
     * @throws {@inheritDoc}
     */
    @Override
    public Short getShort(int n, int radix) throws IOException {
        String value = getString(n);
        return value == null ? null : Short.parseShort(value, radix);
    }

    /**
     * {@inheritDoc}
     *
     * @param n {@inheritDoc}
     * @return {@inheritDoc}
     * @throws {@inheritDoc}
     */
    @Override
    public Integer getUnsignedInteger(int n) throws IOException {
        String value = getString(n);
        return value == null ? null : Integer.parseUnsignedInt(value);
    }

    /**
     * {@inheritDoc}
     *
     * @param n {@inheritDoc}
     * @return {@inheritDoc}
     * @throws {@inheritDoc}
     */
    @Override
    public Long getUnsignedLong(int n) throws IOException {
        String value = getString(n);
        return value == null ? null : Long.parseUnsignedLong(value);
    }

    private void growCoordinates() {
        coordinates = Arrays.copyOf(coordinates, coordinates.length << 1);
    }

    private void growExclusions() {
        exclusions = Arrays.copyOf(exclusions, exclusions.length << 1);
    }

    private char[] getStagingBuffer(int requiredCapacity) {
        if (requiredCapacity > staging.length) {
            staging = new char[Integer.highestOneBit(requiredCapacity - 1) << 1];
        }
        return staging;
    }

    private void noSuchFieldException(int n) throws IOException {
        throw new NoSuchFieldException(n);
    }
}
