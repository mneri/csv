package me.mneri.csv;

public class NotEnoughFieldsException extends IllegalCsvFormatException {
    NotEnoughFieldsException(int line, int expected, int found) {
        super(line, String.format("not enough fields (expected %d, found %d).", expected, found));
    }
}
