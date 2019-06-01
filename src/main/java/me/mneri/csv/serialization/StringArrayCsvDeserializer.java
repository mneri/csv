package me.mneri.csv.serialization;

import me.mneri.csv.CsvDeserializer;
import me.mneri.csv.RecyclableCsvLine;

public class StringArrayCsvDeserializer implements CsvDeserializer<String[]> {
    @Override
    public String[] deserialize(RecyclableCsvLine line) {
        int fields = line.getFieldCount();
        String[] array = new String[fields];

        for (int i = 0; i < fields; i++) {
            array[i] = line.getString(i);
        }

        return array;
    }
}
