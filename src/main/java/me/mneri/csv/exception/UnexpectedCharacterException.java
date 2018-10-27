package me.mneri.csv.exception;

import me.mneri.csv.util.CharUtils;

public class UnexpectedCharacterException extends IllegalCsvFormatException {
    public UnexpectedCharacterException(int line, int c) {
        super(line, String.format("unexpected character '%s'.", CharUtils.printable(c)));
    }
}
