package me.mneri.csv;

import java.util.List;

public class StringListSerializer implements CsvSerializer<List<String>> {
    @Override
    public void serialize(List<String> list, List<String> out) {
        out.addAll(list);
    }
}
