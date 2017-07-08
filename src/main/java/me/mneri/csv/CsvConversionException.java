package me.mneri.csv;

import java.util.List;

public class CsvConversionException extends CsvException {
    CsvConversionException(List<String> fields, Throwable cause) {
        super(String.format("Error while converting values: %s", fields.toString()), cause);
    }
}
