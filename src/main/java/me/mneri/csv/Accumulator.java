package me.mneri.csv;

import java.util.Arrays;

class Accumulator {
    private static final int DEFAULT_BUFFER_SIZE = 256;

    private char[] value = new char[DEFAULT_BUFFER_SIZE];
    private int count;

    void clear() {
        count = 0;
    }

    private void ensureCapacity(int minimumCapacity) {
        int oldCapacity = value.length;

        if (minimumCapacity - oldCapacity <= 0) {
            return;
        }

        int newCapacity = oldCapacity * 2 + 2;

        if (newCapacity < 0) {
            newCapacity = Integer.MAX_VALUE;
        }

        value = Arrays.copyOf(value, newCapacity);
    }

    void put(int codePoint) {
        ensureCapacity(count + 2);
        count += Character.toChars(codePoint, value, count);
    }

    @Override
    public String toString() {
        return new String(value, 0, count);
    }
}
