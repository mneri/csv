package me.mneri.csv.reader;

import java.util.ArrayList;
import java.util.List;

public class StringListDeserializer implements Deserializer<List<String>> {
    @Override
    public List<String> deserialize(RecycledLine line) {
        final int len = line.getFieldCount();
        List<String> list = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            list.add(line.getString(i));
        }
        return list;
    }
}
