package me.mneri.csv;

import java.util.Arrays;

public class ImmutableCsvLine extends RecyclableCsvLine {
    ImmutableCsvLine(RecyclableCsvLine source) {
        //@formatter:off
        super(Arrays.copyOf(source.chars,   source.getCharBufferLength()),
              Arrays.copyOf(source.endings, source.getEndingBufferLength()));
        //@formatter:on
    }

    void append(char c) {
        throw new UnsupportedOperationException();
    }

    void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    int getCharBufferLength() {
        return chars.length;
    }

    @Override
    int getEndingBufferLength() {
        return endings.length;
    }

    void markField() {
        throw new UnsupportedOperationException();
    }
}
