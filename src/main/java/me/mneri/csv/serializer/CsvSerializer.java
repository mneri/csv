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

package me.mneri.csv.serializer;

import me.mneri.csv.deserializer.Deserializer;

import java.util.List;

/**
 * Serialize objects.
 *
 * @param <T> the type of the objects.
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 * @see Deserializer
 */
public interface CsvSerializer<T> {
    /**
     * Serialize an object into a list of strings. The strings should be added in order to the list passed as parameter.
     *
     * @param object the object to serialize.
     * @param out    the list of strings representing the csv line.
     * @throws Exception if anything goes wrong.
     */
    void serialize(T object, List<String> out) throws Exception;
}
