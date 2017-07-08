package me.mneri.csv;

import java.util.List;

public interface CsvConverter<T> {
    T toObject(List<String> values);

    void toList(T object, List<String> out);
}
