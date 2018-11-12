package me.mneri.csv;

public class CsvOptions {
    private static final char DEFAULT_DELIMITER = ',';
    private static final char DEFAULT_QUOTATION = '"';

    private char delimiter = DEFAULT_DELIMITER;
    private char quotation = DEFAULT_QUOTATION;

    char getDelimiter() {
        return delimiter;
    }

    char getQuotation() {
        return quotation;
    }

    public void setDelimiter(char delimiter) {
        this.delimiter = delimiter;
    }

    public void setQuotation(char quotation) {
        this.quotation = quotation;
    }
}
