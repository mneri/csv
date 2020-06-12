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

package me.mneri.csv.writer;

import me.mneri.csv.option.CsvOptions;
import me.mneri.csv.serializer.CsvSerializer;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;

/**
 * Default implementation of {@link CsvWriterFactory}.
 *
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public class DefaultCsvWriterFactory implements CsvWriterFactory {
    /**
     * {@inheritDoc}
     */
    @Override
    public <T> CsvWriter<T> open(File file, Charset charset, CsvSerializer<T> serializer) throws IOException {
        return open(file, charset, CsvOptions.defaultOptions(), serializer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> CsvWriter<T> open(File file, Charset charset, CsvOptions options, CsvSerializer<T> serializer)
            throws IOException {
        return open(Files.newBufferedWriter(file.toPath(), charset), options, serializer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> CsvWriter<T> open(File file, CsvSerializer<T> serializer) throws IOException {
        return open(file, CsvOptions.defaultOptions(), serializer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> CsvWriter<T> open(File file, CsvOptions options, CsvSerializer<T> serializer) throws IOException {
        return open(file, Charset.defaultCharset(), options, serializer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> CsvWriter<T> open(Writer writer, CsvSerializer<T> serializer) {
        return open(writer, CsvOptions.defaultOptions(), serializer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> CsvWriter<T> open(Writer writer, CsvOptions options, CsvSerializer<T> serializer) {
        return new CsvWriterImpl<>(writer, options, serializer);
    }
}
