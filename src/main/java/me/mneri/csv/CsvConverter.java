package me.mneri.csv;

public abstract class CsvConverter {
    public Object fromString(int column, String value) {
        return value;
    }

    public String toString(int column, Object value) {
        return value != null ? value.toString() : null;
    }
}
