package me.mneri.csv.exception;

public class NoSuchFieldException extends UncheckedCsvException {
    public NoSuchFieldException(String message) {
        super(message);
    }
}
