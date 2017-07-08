package me.mneri.csv;

public class UnexpectedCharacterException extends IllegalCsvFormatException {
    UnexpectedCharacterException(int line, int c) {
        super(line, String.format("unexpected character '%c'.", (char) c));
    }
}
