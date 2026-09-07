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

package me.mneri.csv.deserializer;

import me.mneri.csv.line.RecycledLine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple implementation of the {@link Deserializer} interface returning a {@code List} of {@code String}s.
 *
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public class StringListDeserializer implements Deserializer<List<String>> {
    @Override
    public List<String> deserialize(RecycledLine line) throws IOException {
        final int len = line.getFieldCount();
        List<String> list = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            list.add(line.getString(i));
        }
        return list;
    }
}
