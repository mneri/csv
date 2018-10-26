package me.mneri.csv.test.serialization;

import me.mneri.csv.CsvSerializer;

import java.util.List;

public class IntegerListSerializer implements CsvSerializer<List<Integer>> {
    @Override
    public void serialize(List<Integer> object, List<String> out) {
        for (Integer i : object) {
            out.add(String.valueOf(i));
        }
    }
}
