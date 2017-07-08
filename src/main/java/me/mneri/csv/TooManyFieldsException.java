package me.mneri.csv;

public class TooManyFieldsException extends IllegalCsvFormatException {
    TooManyFieldsException(int line, int expected, int found) {
        super(line, String.format("too many fields (expected %d, found %d).", expected, found));
    }
}
