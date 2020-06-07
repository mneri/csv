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
import me.mneri.csv.serialize.CsvSerializer;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;

public interface CsvWriterFactory {
    /**
     * Opens or creates a file for writing, returning a {@code CsvWriter} that may be used to write object to the file
     * in csv format. The file is opened for writing, created if it doesn't exist or initially truncated to a size of 0
     * if it exists. Characters are encoded using the specified charset. Objects are serialized using the specified
     * serializer.
     *
     * @param file       the file to open.
     * @param charset    the charset to use for encoding.
     * @param serializer the serializer used to convert objects into csv lines.
     * @param <T>        the type of the objects to serialize.
     * @return A new {@code CsvWriter} to write into the specified file.
     * @throws IOException if an I/O error occurs.
     */
    <T> CsvWriter<T> open(File file, Charset charset, CsvSerializer<T> serializer) throws IOException;

    /**
     * Opens or creates a file for writing, returning a {@code CsvWriter} that may be used to write object to the file
     * in csv format. The file is opened for writing, created if it doesn't exist or initially truncated to a size of 0
     * if it exists. Characters are encoded using the specified charset. Objects are serialized using the specified
     * serializer.
     *
     * @param file       the file to open.
     * @param charset    the charset to use for encoding.
     * @param options    writing options.
     * @param serializer the serializer used to convert objects into csv lines.
     * @param <T>        the type of the objects to serialize.
     * @return A new {@code CsvWriter} to write into the specified file.
     * @throws IOException if an I/O error occurs.
     */
    <T> CsvWriter<T> open(File file, Charset charset, CsvOptions options, CsvSerializer<T> serializer) throws IOException;

    /**
     * Opens or creates a file for writing, returning a {@code CsvWriter} that may be used to write object to the file
     * in csv format. The file is opened for writing, created if it doesn't exist or initially truncated to a size of 0
     * if it exists. Characters are encoded using the default JVM charset. Objects are serialized using the specified
     * serializer.
     *
     * @param file       the file to open.
     * @param serializer the serializer used to convert objects into csv lines.
     * @param <T>        the type of the objects to serialize.
     * @return A new {@code CsvWriter} to write into the specified file.
     * @throws IOException if an I/O error occurs.
     */
    <T> CsvWriter<T> open(File file, CsvSerializer<T> serializer) throws IOException;

    /**
     * Opens or creates a file for writing, returning a {@code CsvWriter} that may be used to write object to the file
     * in csv format. The file is opened for writing, created if it doesn't exist or initially truncated to a size of 0
     * if it exists. Characters are encoded using the default JVM charset. Objects are serialized using the specified
     * serializer.
     *
     * @param file       the file to open.
     * @param options    writing options.
     * @param serializer the serializer used to convert objects into csv lines.
     * @param <T>        the type of the objects to serialize.
     * @return A new {@code CsvWriter} to write into the specified file.
     * @throws IOException if an I/O error occurs.
     */
    <T> CsvWriter<T> open(File file, CsvOptions options, CsvSerializer<T> serializer) throws IOException;

    /**
     * Return a new {@code CsvWriter} using the specified {@link Writer} for writing. Bytes file are encoded into
     * characters using the writer's charset. Writing commences at the point specified by the reader.
     *
     * @param writer     the {@link Writer} used to write.
     * @param serializer the serializer used to convert objects into csv lines.
     * @param <T>        the type of the objects to serialize.
     * @return A new {@code CsvWriter} to write into the specified file.
     */
    <T> CsvWriter<T> open(Writer writer, CsvSerializer<T> serializer);

    /**
     * Return a new {@code CsvWriter} using the specified {@link Writer} for writing. Bytes file are encoded into
     * characters using the writer's charset. Writing commences at the point specified by the reader.
     *
     * @param writer     the {@link Writer} used to write.
     * @param options    writing options.
     * @param serializer the serializer used to convert objects into csv lines.
     * @param <T>        the type of the objects to serialize.
     * @return A new {@code CsvWriter} to write into the specified file.
     */
    <T> CsvWriter<T> open(Writer writer, CsvOptions options, CsvSerializer<T> serializer);
}
