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

/**
 * Simple implementation of the {@link Deserializer} interface returning an array of {@code String}s.
 *
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public class StringArrayDeserializer implements Deserializer<String[]> {
    @Override
    public String[] deserialize(RecycledLine line) throws IOException {
        final int len = line.getFieldCount();
        String[] list = new String[len];
        for (int i = 0; i < len; i++) {
            list[i] = line.getString(i);
        }
        return list;
    }
}
