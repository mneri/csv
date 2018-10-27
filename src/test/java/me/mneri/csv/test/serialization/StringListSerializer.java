package me.mneri.csv.test.serialization;

import me.mneri.csv.CsvSerializer;

import java.util.List;

public class StringListSerializer implements CsvSerializer<List<String>> {
    @Override
    public void serialize(List<String> list, List<String> out) {
        out.addAll(list);
    }
}
