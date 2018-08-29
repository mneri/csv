package me.mneri.csv.test;

import java.util.List;
import me.mneri.csv.CsvConverter;

public class VoidConverter implements CsvConverter<Void> {
    @Override
    public void toCsvLine(Void object, List<String> out) {
        // Do nothing
    }

    @Override
    public Void toObject(List<String> line) {
        return null;
    }
}
