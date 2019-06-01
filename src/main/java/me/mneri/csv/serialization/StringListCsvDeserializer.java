package me.mneri.csv.serialization;

import me.mneri.csv.CsvDeserializer;
import me.mneri.csv.RecyclableCsvLine;

import java.util.ArrayList;
import java.util.List;

public class StringListCsvDeserializer implements CsvDeserializer<List<String>> {
    @Override
    public List<String> deserialize(RecyclableCsvLine line) {
        int fields = line.getFieldCount();
        List<String> list = new ArrayList<>(fields);

        for (int i = 0; i < fields; i++) {
            list.add(line.getString(i));
        }

        return list;
    }
}
