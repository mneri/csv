package me.mneri.csv;

import java.util.List;

public interface CsvConverter<T> {
    T toObject(List<String> line);

    void toCsvLine(T object, List<String> out);
}
