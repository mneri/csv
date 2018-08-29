package me.mneri.csv.test;

import java.util.List;
import me.mneri.csv.CsvConverter;

public class ExceptionConverter implements CsvConverter<Void> {
    @Override
    public Void toObject(List<String> line) {
        throw new RuntimeException();
    }

    @Override
    public void toCsvLine(Void object, List<String> out) {
        throw new RuntimeException();
    }
}
