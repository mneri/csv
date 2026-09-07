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

public final class InternalRecycledLine implements RecycledLine {
    private final RandomAccessStream reader;

    private long[] idx = new long[768];
    private int idxSize;

    private long[] drt = new long[32];
    private int drtSize;

    private char[] buff = new char[256];

    public InternalRecycledLine(RandomAccessStream reader) {
        if (reader == null) {
            throw new IllegalArgumentException("Reader cannot be null");
        }
        this.reader = reader;
    }

    public void startField(long pos) {
        if (idxSize + 3 > idx.length) {
            growIdx();
        }
        idx[idxSize] = pos;
        idx[idxSize + 2] = -1; // Initially marked as not dirty
    }

    private void growIdx() {
        idx = Arrays.copyOf(idx, idx.length << 1);
    }

    public void endField(long pos) {
        idx[idxSize + 1] = pos;
        idxSize += 3;
    }

    public void dirty(long pos) {
        if (idx[idxSize + 2] == -1) {
            idx[idxSize + 2] = drtSize;
        }
        if (drtSize >= drt.length) {
            growDrt(); // Pushed to cold path
        }
        drt[drtSize++] = pos;
    }

    private void growDrt() {
        drt = Arrays.copyOf(drt, drt.length << 1);
    }

    public void reset() {
        idxSize = 0;
        drtSize = 0;
    }

    private char[] ensureBuffCapacity(int required) {
        if (buff.length < required) {
            // Bit-twiddling hack to scale to the next highest power of two.
            // Avoids thrashing allocations if field lengths creep up slowly.
            int nextPow2 = Integer.highestOneBit(required - 1) << 1;
            buff = new char[nextPow2];
        }
        return buff;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public int getFieldCount() {
        return idxSize / 3;
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
        if (n >= idxSize) {
            noSuchFieldException(n);
        }
        int i = n * 3;
        if (idx[i + 2] == -1) { // If the field is marked NOT dirty
            return reader.getString(idx[i], idx[i + 1]);
        }
        return getDirtyString(n);
    }

    private String getDirtyString(int n) throws IOException {
        int length = getDirtyCharArray(n, buff, 0);
        return new String(buff, 0, length);
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
        int length = getCharArray(n, buff, 0);
        return new BigDecimal(buff, 0, length);
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
        if (n >= idxSize) {
            noSuchFieldException(n);
        }
        int i = n * 3;
        if (idx[i + 2] == -1) { // If the field is marked NOT dirty
            long start = idx[i];
            long end = idx[i + 1];
            reader.getCharArray(dest, destPos, start, end);
            return (int) (end - start);
        }
        return getDirtyCharArray(n, dest, destPos);
    }

    private int getDirtyCharArray(int n, char[] dest, int destPos) throws IOException {
        int i = n * 3;

        long start = idx[i];
        long end = idx[i + 1];
        int copied = 0;

        int j = (int) idx[i + 2];
        while (j < drtSize && drt[j] < end) {
            long stop = drt[j++];
            reader.getCharArray(dest, destPos + copied, start, stop);
            copied += (int) (stop - start);
            start = stop + 1;
        }

        reader.getCharArray(dest, destPos + copied, start, end);
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

    private void noSuchFieldException(int n) throws IOException {
        throw new NoSuchFieldException("No such field: " + n);
    }
}
