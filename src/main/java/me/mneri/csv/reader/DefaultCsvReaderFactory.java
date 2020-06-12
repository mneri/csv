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

package me.mneri.csv.reader;

import me.mneri.csv.deserializer.CsvDeserializer;
import me.mneri.csv.option.CsvOptions;

import java.io.*;
import java.nio.charset.Charset;

public class DefaultCsvReaderFactory implements CsvReaderFactory {
    /**
     * {@inheritDoc}
     */
    public <T> CsvReader<T> open(File file, CsvDeserializer<T> deserializer) throws IOException {
        return open(file, CsvOptions.defaultOptions(), deserializer);
    }

    /**
     * {@inheritDoc}
     */
    public <T> CsvReader<T> open(File file, CsvOptions options, CsvDeserializer<T> deserializer) throws IOException {
        return open(file, Charset.defaultCharset(), options, deserializer);
    }

    /**
     * {@inheritDoc}
     */
    public <T> CsvReader<T> open(File file, Charset charset, CsvDeserializer<T> deserializer) throws IOException {
        return open(file, charset, CsvOptions.defaultOptions(), deserializer);
    }

    /**
     * {@inheritDoc}
     */
    public <T> CsvReader<T> open(File file, Charset charset, CsvOptions options, CsvDeserializer<T> deserializer)
            throws IOException {
        return open(new InputStreamReader(new FileInputStream(file), charset), options, deserializer);
    }

    /**
     * {@inheritDoc}
     */
    public <T> CsvReader<T> open(Reader reader, CsvDeserializer<T> deserializer) {
        return open(reader, CsvOptions.defaultOptions(), deserializer);
    }

    /**
     * {@inheritDoc}
     */
    public <T> CsvReader<T> open(Reader reader, CsvOptions options, CsvDeserializer<T> deserializer) {
        return new CsvReaderImpl<>(reader, options, deserializer);
    }
}
