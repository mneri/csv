package me.mneri.csv.exception;

import java.util.List;

public class CsvConversionException extends CsvException {
    public CsvConversionException(List<String> fields, Throwable cause) {
        super(String.format("Error while converting values: %s", fields.toString()), cause);
    }
}
