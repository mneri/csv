package me.mneri.csv;

import java.util.List;

public interface CsvSerializer<T> {
    void serialize(T object, List<String> out) throws Exception;
}
