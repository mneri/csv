package me.mneri.csv;

import java.util.Objects;

public class UncheckedCsvException extends RuntimeException {
    UncheckedCsvException(CsvException cause) {
        super(cause.getMessage(), Objects.requireNonNull(cause));
    }
}
