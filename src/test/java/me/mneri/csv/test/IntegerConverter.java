package me.mneri.csv.test;

import java.util.ArrayList;
import java.util.List;
import me.mneri.csv.CsvConverter;

public class IntegerConverter implements CsvConverter<List<Integer>> {
    @Override
    public void toCsvLine(List<Integer> ints, List<String> out) {
        for (Integer i : ints)
            out.add(String.valueOf(i));
    }

    @Override
    public List<Integer> toObject(List<String> line) {
        List<Integer> ints = new ArrayList<>(line.size());

        for (String string : line)
            ints.add(Integer.valueOf(string));

        return ints;
    }
}
