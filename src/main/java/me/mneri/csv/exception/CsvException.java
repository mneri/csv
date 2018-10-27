package me.mneri.csv.exception;

public class CsvException extends Exception {
    CsvException(String message, Throwable cause) {
        super(message, cause);
    }

    CsvException(String message) {
        super(message);
    }
}
