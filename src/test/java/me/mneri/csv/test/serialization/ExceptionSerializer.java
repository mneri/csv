package me.mneri.csv.test.serialization;

import me.mneri.csv.CsvSerializer;
import me.mneri.csv.test.exception.DummyException;

import java.util.List;

public class ExceptionSerializer implements CsvSerializer<Void> {
    @Override
    public void serialize(Void object, List<String> out) throws DummyException {
        throw new DummyException();
    }
}
