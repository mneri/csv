package me.mneri.csv.serialization;

import me.mneri.csv.CsvSerializer;

import java.util.List;

public class StringArrayCsvSerializer implements CsvSerializer<String[]> {
    @Override
    public void serialize(String[] strings, List<String> out) {
        for (int i = 0; i < strings.length; i++) {
            out.add(strings[i]);
        }
    }
}
