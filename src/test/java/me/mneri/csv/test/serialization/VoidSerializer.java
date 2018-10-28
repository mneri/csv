package me.mneri.csv.test.serialization;

import me.mneri.csv.CsvSerializer;

import java.util.List;

public class VoidSerializer implements CsvSerializer<Void> {
    @Override
    public void serialize(Void object, List<String> out) {
        // Do nothing.
    }
}
