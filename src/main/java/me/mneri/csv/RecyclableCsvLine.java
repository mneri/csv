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

package me.mneri.csv;

import me.mneri.csv.exception.CsvConversionException;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * A csv line which is generated by {@link CsvReader}. This class provides getter methods
 * ({@link RecyclableCsvLine#getBoolean}, {@link RecyclableCsvLine#getLong}, and so on) for retrieving column values.
 * Instances of this class shouldn't be used outside the scope of the method they're provided to because instances may
 * be cleaned and reused by {@link CsvReader}.
 *
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public final class RecyclableCsvLine {
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    private char[] chars;
    private int[] ends;
    private int fieldCount;
    private int nextChar;
    private int nextEnd;

    RecyclableCsvLine() {
        chars = new char[DEFAULT_BUFFER_SIZE];
        ends = new int[DEFAULT_BUFFER_SIZE];
    }

    private void checkField(int i) {
        if (i >= fieldCount) {
            throw new ArrayIndexOutOfBoundsException(i);
        }
    }

    void clear() {
        fieldCount = nextChar = nextEnd = 0;
    }

    private void ensureCapacity(int minimumCapacity) {
        int oldCapacity = chars.length;

        if (minimumCapacity - oldCapacity <= 0) {
            return;
        }

        int newCapacity = oldCapacity * 2 + 2;

        if (newCapacity < 0) {
            newCapacity = Integer.MAX_VALUE;
        }

        chars = Arrays.copyOf(chars, newCapacity);
        ends = Arrays.copyOf(ends, newCapacity);
    }

    /**
     * Return the value of the field at the specified index as {@link BigDecimal}.
     *
     * @param i The index of the field.
     * @return The value of the field.
     */
    public BigDecimal getBigDecimal(int i) {
        checkField(i);

        int length = getFieldLength(i);

        if (length == 0) {
            return null;
        }

        return new BigDecimal(chars, getFieldStart(i), length);
    }

    public BigInteger getBigInteger(int i) {
        return getBigInteger(i, 10);
    }

    public BigInteger getBigInteger(int i, int radix) {
        String value = getString(i);
        return value == null ? null : new BigInteger(value, radix);
    }

    /**
     * Return the value of the field at the specified index as {@link Boolean}.
     *
     * @param i The index of the field.
     * @return The value of the field.
     */
    public Boolean getBoolean(int i) {
        String value = getString(i);
        return value == null ? null : Boolean.parseBoolean(value);
    }

    /**
     * Return the value of the field at the specified index as {@link Double}.
     *
     * @param i The index of the field.
     * @return The value of the field.
     */
    public Double getDouble(int i) {
        String value = getString(i);
        return value == null ? null : Double.parseDouble(value);
    }

    /**
     * Return the number of fields in this line.
     *
     * @return The number of fields.
     */
    public int getFieldCount() {
        return fieldCount;
    }

    private int getFieldLength(int i) {
        return i == 0 ? ends[0] : ends[i] - ends[i - 1];
    }

    private int getFieldStart(int i) {
        return i == 0 ? 0 : ends[i - 1];
    }

    /**
     * Return the value of the field at the specified index as {@link Float}.
     *
     * @param i The index of the field.
     * @return The value of the field.
     */
    public Float getFloat(int i) {
        String value = getString(i);
        return value == null ? null : Float.parseFloat(value);
    }

    /**
     * Return the value of the field at the specified index as {@link Integer}.
     *
     * @param i The index of the field.
     * @return The value of the field.
     */
    public Integer getInteger(int i) {
        return getInteger(i, 10);
    }

    /**
     * Return the value of the field at the specified index as {@link Integer} in the radix specified by the second
     * argument. The characters in the string must all be digits of the specified radix, except that the first character
     * may be an ASCII minus sign {@code '-'} an ASCII plus sign {@code '+'}.
     *
     * @param i     The index of the field.
     * @param radix The radix to be used.
     * @return The value of the field.
     */
    public Integer getInteger(int i, int radix) {
        String value = getString(i);
        return value == null ? null : Integer.parseInt(value, radix);
    }

    /**
     * Return the value of the field at the specified index as {@link Long}.
     *
     * @param i The index of the field.
     * @return The value of the field.
     */
    public Long getLong(int i) {
        return getLong(i, 10);
    }

    /**
     * Return the value of the field at the specified index as {@link Long} in the radix specified by the second
     * argument. The characters in the string must all be digits of the specified radix, except that the first character
     * may be an ASCII minus sign {@code '-'} an ASCII plus sign {@code '+'}.
     *
     * @param i     The index of the field.
     * @param radix The radix to be used.
     * @return The value of the field.
     */
    public Long getLong(int i, int radix) {
        String value = getString(i);
        return value == null ? null : Long.parseLong(value, radix);
    }

    /**
     * Return the value of the field at the specified index as {@link Short}.
     *
     * @param i The index of the field.
     * @return The value of the field.
     */
    public Short getShort(int i) {
        return getShort(i, 10);
    }

    /**
     * Return the value of the field at the specified index as {@link Short} in the radix specified by the second
     * argument. The characters in the string must all be digits of the specified radix, except that the first character
     * may be an ASCII minus sign {@code '-'} an ASCII plus sign {@code '+'}.
     *
     * @param i     The index of the field.
     * @param radix The radix to be used.
     * @return The value of the field.
     */
    public Short getShort(int i, int radix) {
        String value = getString(i);
        return value == null ? null : Short.parseShort(value, radix);
    }

    /**
     * Return the value of the field at the specified index as {@link String}.
     *
     * @param i The index of the field.
     * @return The value of the field.
     */
    public String getString(int i) {
        checkField(i);

        int length = getFieldLength(i);

        if (length == 0) {
            return null;
        }

        return new String(chars, getFieldStart(i), length);
    }

    void markField() {
        ends[nextEnd++] = nextChar;
        fieldCount++;
    }

    void put(int codePoint) {
        ensureCapacity(nextChar + 2);
        nextChar += Character.toChars(codePoint, chars, nextChar);
    }

    @Override
    public String toString() {
        StringWriter buffer = new StringWriter();
        CsvSerializer<RecyclableCsvLine> serializer = (line, out) -> {
            int count = line.getFieldCount();

            for (int i = 0; i < count; i++) {
                out.add(line.getString(i));
            }
        };

        try (CsvWriter<RecyclableCsvLine> writer = CsvWriter.open(buffer, serializer)) {
            writer.put(this);
            return buffer.toString();
        } catch (CsvConversionException | IOException ignored) {
            return null;
        }
    }
}
