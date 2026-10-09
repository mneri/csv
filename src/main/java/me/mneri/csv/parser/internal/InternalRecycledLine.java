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

package me.mneri.csv.parser.internal;

import ch.randelshofer.fastdoubleparser.JavaBigDecimalParser;
import ch.randelshofer.fastdoubleparser.JavaBigIntegerParser;
import ch.randelshofer.fastdoubleparser.JavaDoubleParser;
import ch.randelshofer.fastdoubleparser.JavaFloatParser;
import me.mneri.csv.exception.NoSuchFieldException;
import me.mneri.csv.parser.RecycledLine;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Internal implementation of the {@link RecycledLine} interface. <i>This class is internal and is not meant to be used
 * by clients.</i>
 * <p>
 * {@code InternalRecycledLine}, {@code Page} and {@code PageParser} implementations are closely related, and together
 * they compose the parsing and data extraction behaviour. As the page is processed, the page parser populates the
 * page's table by recording positional markers via calls to {@link Page#startField(int)}, {@link Page#endField(int)},
 * and {@link Page#exclude(int)}. Rather than eagerly allocating or copying strings, {@code InternalRecycledLine} only
 * points at one line of the table. When a field value is subsequently requested by the client (for example, via
 * {@link #getString(int)}), {@code InternalRecycledLine} uses the saved markers to read and assemble the data directly
 * from the page's buffer on demand.
 *
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public final class InternalRecycledLine implements RecycledLine {
    private final Page page;
    private final char[] buf;
    private final int[] fields;
    private final int[] lines;
    private int from;         // This line's fields: the pairs in fields[from, to)
    private int to;
    private int excludedFrom; // This line's excluded characters: page.excluded[excludedFrom, excludedTo)
    private int excludedTo;

    private char[] staging = new char[512];

    InternalRecycledLine(Page page) {
        this.page = page;
        this.buf = page.buffer;
        this.fields = page.fields;
        this.lines = page.lines;
    }

    InternalRecycledLine moveTo(int i) {
        from = lines[2 * i];
        excludedFrom = lines[2 * i + 1];
        to = lines[2 * i + 2];
        excludedTo = lines[2 * i + 3];
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public int getFieldCount() {
        return (to - from) >> 1;
    }

    /**
     * {@inheritDoc}
     *
     * @param n {@inheritDoc}
     * @return {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    @Override
    public String getString(int n) throws IOException {
        if (n < 0 || n >= getFieldCount()) {
            noSuchFieldException(n);
        }
        int i = from + 2 * n;
        int length = fieldLength(i);
        if (length == 0) {
            return null;
        }
        if (!isDirty()) {
            return new String(buf, fieldStart(i), length);
        }
        return getDirtyString(n, length);
    }

    private String getDirtyString(int n, int length) throws IOException {
        char[] buffer = getStagingBuffer(length);
        int actualLength = getDirtyCharArray(n, buffer, 0);
        return new String(buffer, 0, actualLength);
    }

    /**
     * {@inheritDoc}
     *
     * @param n {@inheritDoc}
     * @return {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    @Override
    public BigDecimal getBigDecimal(int n) throws IOException {
        if (n < 0 || n >= getFieldCount()) {
            noSuchFieldException(n);
        }
        int i = from + 2 * n;
        int length = fieldLength(i);
        if (length == 0) {
            return null;
        }
        if (!isDirty()) {
            return JavaBigDecimalParser.parseBigDecimal(buf, fieldStart(i), length);
        }
        return getDirtyBigDecimal(n, length);
    }

    private BigDecimal getDirtyBigDecimal(int n, int length) throws IOException {
        char[] buffer = getStagingBuffer(length);
        int actualLength = getDirtyCharArray(n, buffer, 0);
        return JavaBigDecimalParser.parseBigDecimal(buffer, 0, actualLength);
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
        if (n < 0 || n >= getFieldCount()) {
            noSuchFieldException(n);
        }
        int i = from + 2 * n;
        int length = fieldLength(i);
        if (length == 0) {
            return null;
        }
        if (!isDirty()) {
            return JavaBigIntegerParser.parseBigInteger(buf, fieldStart(i), length, radix);
        }
        return getDirtyBigInteger(n, length, radix);
    }

    private BigInteger getDirtyBigInteger(int n, int length, int radix) throws IOException {
        char[] buffer = getStagingBuffer(length);
        int actualLength = getDirtyCharArray(n, buffer, 0);
        return JavaBigIntegerParser.parseBigInteger(buffer, 0, actualLength, radix);
    }

    /**
     * {@inheritDoc}
     *
     * @param n {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public boolean getBoolean(int n, boolean def) throws IOException {
        String value = getString(n);
        return value == null ? def : Boolean.parseBoolean(value);
    }

    @Override
    public int getCharArray(int n, char[] dest, int destPos) throws IOException {
        if (n < 0 || n >= getFieldCount()) {
            noSuchFieldException(n);
        }
        int i = from + 2 * n;
        if (!isDirty()) {
            int start = fieldStart(i);
            int length = fieldLength(i);
            System.arraycopy(buf, start, dest, destPos, length);
            return length;
        }
        return getDirtyCharArray(n, dest, destPos);
    }

    private int getDirtyCharArray(int n, char[] dest, int destPos) throws IOException {
        final int[] excluded = page.excluded;
        int i = from + 2 * n;

        int start = fields[i];
        int end = fields[i + 1];
        int copied = 0;

        int j = excludedFrom;
        while (j < excludedTo && excluded[j] < start) {
            j++;
        }
        while (j < excludedTo && excluded[j] < end) {
            int stop = excluded[j++];
            int length = stop - start;
            System.arraycopy(buf, start, dest, destPos + copied, length);
            copied += length;
            start = stop + 1;
        }

        int length = end - start;
        System.arraycopy(buf, start, dest, destPos + copied, length);
        copied += length;

        return copied;
    }

    /**
     * {@inheritDoc}
     *
     * @param n {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public double getDouble(int n, double def) throws IOException {
        if (n < 0 || n >= getFieldCount()) {
            noSuchFieldException(n);
        }
        int i = from + 2 * n;
        int length = fieldLength(i);
        if (length == 0) {
            return def;
        }
        if (!isDirty()) {
            return JavaDoubleParser.parseDouble(buf, fieldStart(i), length);
        }
        return getDirtyDouble(n, length);
    }

    private double getDirtyDouble(int n, int length) throws IOException {
        char[] buffer = getStagingBuffer(length);
        int actualLength = getDirtyCharArray(n, buffer, 0);
        return JavaDoubleParser.parseDouble(buffer, 0, actualLength);
    }

    /**
     * {@inheritDoc}
     *
     * @param n {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public float getFloat(int n, float def) throws IOException {
        if (n < 0 || n >= getFieldCount()) {
            noSuchFieldException(n);
        }
        int i = from + 2 * n;
        int length = fieldLength(i);
        if (length == 0) {
            return def;
        }
        if (!isDirty()) {
            return JavaFloatParser.parseFloat(buf, fieldStart(i), length);
        }
        return getDirtyFloat(n, length);
    }

    private float getDirtyFloat(int n, int length) throws IOException {
        char[] buffer = getStagingBuffer(length);
        int actualLength = getDirtyCharArray(n, buffer, 0);
        return JavaFloatParser.parseFloat(buffer, 0, actualLength);
    }

    /**
     * {@inheritDoc}
     *
     * @param n {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public int getInteger(int n, int def) throws IOException {
        return getInteger(n, 10, def);
    }

    /**
     * {@inheritDoc}
     *
     * @param n     {@inheritDoc}
     * @param radix {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public int getInteger(int n, int radix, int def) throws IOException {
        String value = getString(n);
        return value == null ? def : Integer.parseInt(value, radix);
    }

    /**
     * {@inheritDoc}
     *
     * @param n {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public long getLong(int n, long def) throws IOException {
        return getLong(n, 10, def);
    }

    /**
     * {@inheritDoc}
     *
     * @param n     {@inheritDoc}
     * @param radix {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public long getLong(int n, int radix, long def) throws IOException {
        String value = getString(n);
        return value == null ? def : Long.parseLong(value, radix);
    }

    /**
     * {@inheritDoc}
     *
     * @param n {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public short getShort(int n, short def) throws IOException {
        return getShort(n, 10, def);
    }

    /**
     * {@inheritDoc}
     *
     * @param n     {@inheritDoc}
     * @param radix {@inheritDoc}
     * @return {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    @Override
    public short getShort(int n, int radix, short def) throws IOException {
        String value = getString(n);
        return value == null ? def : Short.parseShort(value, radix);
    }

    /**
     * {@inheritDoc}
     *
     * @param n {@inheritDoc}
     * @return {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    @Override
    public int getUnsignedInteger(int n, int def) throws IOException {
        String value = getString(n);
        return value == null ? def : Integer.parseUnsignedInt(value);
    }

    /**
     * {@inheritDoc}
     *
     * @param n {@inheritDoc}
     * @return {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    @Override
    public long getUnsignedLong(int n, long def) throws IOException {
        String value = getString(n);
        return value == null ? def : Long.parseUnsignedLong(value);
    }

    private int fieldStart(int i) {
        return fields[i];
    }

    private int fieldLength(int i) {
        return fields[i + 1] - fields[i];
    }

    private boolean isDirty() {
        return excludedFrom != excludedTo;
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
