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

package me.mneri.csv.reader;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Default implementation of {@link RecyclableCsvLine}.
 *
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
class RecyclableCsvLineImpl implements RecyclableCsvLine {
    final char[] chars;
    final int[] endings;
    private int nextChar;
    private int nextEnding;

    RecyclableCsvLineImpl(int capacity) {
        this(new char[capacity], new int[capacity]);
    }

    RecyclableCsvLineImpl(char[] chars, int[] endings) {
        this.chars = chars;
        this.endings = endings;
    }

    void append(char c) {
        chars[nextChar++] = c;
    }

    void clear() {
        nextChar = nextEnding = 0;
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal getBigDecimal(int i) {
        String value = getString(i);
        return value == null ? null : new BigDecimal(value);
    }

    /**
     * {@inheritDoc}
     */
    public BigInteger getBigInteger(int i) {
        return getBigInteger(i, 10);
    }

    /**
     * {@inheritDoc}
     */
    public BigInteger getBigInteger(int i, int radix) {
        String value = getString(i);
        return value == null ? null : new BigInteger(value, radix);
    }

    /**
     * {@inheritDoc}
     */
    public Boolean getBoolean(int i) {
        String value = getString(i);
        return value == null ? null : Boolean.parseBoolean(value);
    }

    int getCharBufferLength() {
        return nextChar;
    }

    /**
     * {@inheritDoc}
     */
    public Double getDouble(int i) {
        String value = getString(i);
        return value == null ? null : Double.parseDouble(value);
    }

    int getEndingBufferLength() {
        return nextEnding;
    }

    /**
     * {@inheritDoc}
     */
    public int getFieldCount() {
        return nextEnding;
    }

    private int getFieldLength(int i) {
        return i == 0 ? endings[0] : endings[i] - endings[i - 1];
    }

    private int getFieldStart(int i) {
        return i == 0 ? 0 : endings[i - 1];
    }

    /**
     * {@inheritDoc}
     */
    public Float getFloat(int i) {
        String value = getString(i);
        return value == null ? null : Float.parseFloat(value);
    }

    /**
     * {@inheritDoc}
     */
    public Integer getInteger(int i) {
        return getInteger(i, 10);
    }

    /**
     * {@inheritDoc}
     */
    public Integer getInteger(int i, int radix) {
        String value = getString(i);
        return value == null ? null : Integer.parseInt(value, radix);
    }

    /**
     * {@inheritDoc}
     */
    public Long getLong(int i) {
        return getLong(i, 10);
    }

    /**
     * {@inheritDoc}
     */
    public Long getLong(int i, int radix) {
        String value = getString(i);
        return value == null ? null : Long.parseLong(value, radix);
    }

    /**
     * {@inheritDoc}
     */
    public Short getShort(int i) {
        return getShort(i, 10);
    }

    /**
     * {@inheritDoc}
     */
    public Short getShort(int i, int radix) {
        String value = getString(i);
        return value == null ? null : Short.parseShort(value, radix);
    }

    /**
     * {@inheritDoc}
     */
    public String getString(int i) {
        int length = getFieldLength(i);

        if (length == 0) {
            return null;
        }

        return new String(chars, getFieldStart(i), length);
    }

    void markField() {
        endings[nextEnding++] = nextChar;
    }
}
