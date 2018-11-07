package me.mneri.csv.test.serialization;

import me.mneri.csv.CsvDeserializer;
import me.mneri.csv.RecyclableCsvLine;

public class VoidDeserializer implements CsvDeserializer<Void> {
    @Override
    public Void deserialize(RecyclableCsvLine line) {
        return null;
    }
}
