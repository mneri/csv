package me.mneri.csv.test.serialization;

import me.mneri.csv.CsvDeserializer;

import java.util.ArrayList;
import java.util.List;

public class IntegerListDeserializer implements CsvDeserializer<List<Integer>> {
    @Override
    public List<Integer> deserialize(List<String> line) {
        List<Integer> integers = new ArrayList<>(line.size());

        for (String value : line) {
            integers.add(value == null ? null : Integer.parseInt(value));
        }

        return integers;
    }
}
