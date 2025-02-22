package me.mneri.csv.reader;

import java.math.BigDecimal;
import java.math.BigInteger;

import me.mneri.csv.exception.NoSuchFieldException;

class RecycledLineImpl implements RecycledLine {
    private final String[] fields = new String[32_768];
    private int size;

    void addField(String s) {
        fields[size++] = s;
    }

    @Override
    public int getFieldCount() {
        return size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString(int i) {
        if (i >= size) {
            throw new NoSuchFieldException("No such field: " + i);
        }
        return fields[i];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal getBigDecimal(int i) {
        String value = getString(i);
        return value == null ? null : new BigDecimal(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger getBigInteger(int i) {
        return getBigInteger(i, 10);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigInteger getBigInteger(int i, int radix) {
        String value = getString(i);
        return value == null ? null : new BigInteger(value, radix);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean getBoolean(int i) {
        String value = getString(i);
        return value == null ? null : Boolean.parseBoolean(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Double getDouble(int i) {
        String value = getString(i);
        return value == null ? null : Double.parseDouble(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Float getFloat(int i) {
        String value = getString(i);
        return value == null ? null : Float.parseFloat(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getInteger(int i) {
        return getInteger(i, 10);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getInteger(int i, int radix) {
        String value = getString(i);
        return value == null ? null : Integer.parseInt(value, radix);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getLong(int i) {
        return getLong(i, 10);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getLong(int i, int radix) {
        String value = getString(i);
        return value == null ? null : Long.parseLong(value, radix);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Short getShort(int i) {
        return getShort(i, 10);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Short getShort(int i, int radix) {
        String value = getString(i);
        return value == null ? null : Short.parseShort(value, radix);
    }

    void reset() {
        size = 0;
    }
}
