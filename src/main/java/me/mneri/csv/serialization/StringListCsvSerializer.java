package me.mneri.csv.serialization;

import me.mneri.csv.CsvSerializer;

import java.util.List;

public class StringListCsvSerializer implements CsvSerializer<List<String>> {
    @Override
    public void serialize(List<String> strings, List<String> out) {
        out.addAll(strings);
    }
}
