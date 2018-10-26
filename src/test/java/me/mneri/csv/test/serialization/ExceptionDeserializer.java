package me.mneri.csv.test.serialization;

import me.mneri.csv.CsvDeserializer;
import me.mneri.csv.test.exception.DummyException;

import java.util.List;

public class ExceptionDeserializer implements CsvDeserializer<Void> {
    @Override
    public Void deserialize(List<String> line) throws DummyException {
        throw new DummyException();
    }
}
