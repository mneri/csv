package me.mneri.csv.exception;

import java.util.Objects;

public class UncheckedCsvException extends RuntimeException {
    public UncheckedCsvException(CsvException cause) {
        super(cause.getMessage(), Objects.requireNonNull(cause));
    }
}
