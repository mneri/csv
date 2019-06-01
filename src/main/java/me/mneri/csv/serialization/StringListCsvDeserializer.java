/*
 * Copyright 2018 Massimo Neri <hello@mneri.me>
 *
 * This file is part of mneri/csv.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
