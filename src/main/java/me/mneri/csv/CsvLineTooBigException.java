package me.mneri.csv;

public class CsvLineTooBigException extends IllegalCsvFormatException {
    CsvLineTooBigException(int line) {
        super(line, "line is too big.");
    }
}
