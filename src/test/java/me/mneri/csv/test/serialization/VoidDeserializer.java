package me.mneri.csv.test.serialization;

import me.mneri.csv.CsvDeserializer;

import java.util.List;

public class VoidDeserializer implements CsvDeserializer<Void> {
    @Override
    public Void deserialize(List<String> line) {
        return null;
    }
}
