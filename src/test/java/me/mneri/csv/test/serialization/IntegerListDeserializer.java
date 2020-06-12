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

package me.mneri.csv.test.serialization;

import me.mneri.csv.deserializer.CsvDeserializer;
import me.mneri.csv.reader.RecyclableCsvLine;

import java.util.ArrayList;
import java.util.List;

public class IntegerListDeserializer implements CsvDeserializer<List<Integer>> {
    @Override
    public List<Integer> deserialize(RecyclableCsvLine line) {
        List<Integer> integers = new ArrayList<>(line.getFieldCount());

        for (int i = 0; i < line.getFieldCount(); i++) {
            String value = line.getString(i);
            integers.add(value == null ? null : Integer.parseInt(value));
        }

        return integers;
    }
}
