package me.mneri.csv;

import java.util.List;

public interface CsvDeserializer<T> {
    T deserialize(List<String> line) throws Exception;
}
