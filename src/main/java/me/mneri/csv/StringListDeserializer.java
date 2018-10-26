package me.mneri.csv;

import java.util.ArrayList;
import java.util.List;

public class StringListDeserializer implements CsvDeserializer<List<String>> {
    @Override
    public List<String> deserialize(List<String> line) {
        return new ArrayList<>(line);
    }
}
