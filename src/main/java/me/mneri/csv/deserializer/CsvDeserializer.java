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

import me.mneri.csv.reader.RecyclableCsvLine;
import me.mneri.csv.serializer.CsvSerializer;

/**
 * Deserialize objects.
 *
 * @param <T> the type of the objects.
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 * @see CsvSerializer
 */
public interface CsvDeserializer<T> {
    /**
     * Deserialize an object starting from csv line. The order of the strings is the same as found in the csv.
     *
     * @param line the csv line.
     * @return An object.
     * @throws Exception if anything goes wrong.
     */
    T deserialize(RecyclableCsvLine line) throws Exception;
}
