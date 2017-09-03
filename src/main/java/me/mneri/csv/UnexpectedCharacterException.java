package me.mneri.csv;

import me.mneri.csv.util.CharUtils;

public class UnexpectedCharacterException extends IllegalCsvFormatException {
    UnexpectedCharacterException(int line, int c) {
        super(line, String.format("unexpected character '%s'.", CharUtils.toString(c)));
    }
}
