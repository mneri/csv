package me.mneri.csv;

public class LineTooBigException extends CsvRuntimeException {
    LineTooBigException() {
        super("The line is too big.");
    }
}
