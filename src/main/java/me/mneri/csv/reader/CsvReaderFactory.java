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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;

public interface CsvReaderFactory {
    /**
     * Opens a file for reading, returning a {@code CsvReader}. Bytes from the file are decoded into characters using
     * the default JVM charset. Reading commences at the beginning of the file.
     *
     * @param file         the file to open.
     * @param deserializer the deserializer used to convert csv lines into objects.
     * @param <T>          the type of the objects to read.
     * @return A new {@code CsvReader} to read the specified file.
     * @throws IOException if an I/O error occurs.
     */
    <T> CsvReader<T> open(File file, CsvDeserializer<T> deserializer) throws IOException;

    /**
     * Opens a file for reading, returning a {@code CsvReader}. Bytes from the file are decoded into characters using
     * the default JVM charset. Reading commences at the beginning of the file.
     *
     * @param file         the file to open.
     * @param options      reading options.
     * @param deserializer the deserializer used to convert csv lines into objects.
     * @param <T>          the type of the objects to read.
     * @return A new {@code CsvReader} to read the specified file.
     * @throws IOException if an I/O error occurs.
     */
    <T> CsvReader<T> open(File file, CsvOptions options, CsvDeserializer<T> deserializer) throws IOException;

    /**
     * Opens a file for reading, returning a {@code CsvReader}. Bytes from the file are decoded into characters using
     * the specified charset. Reading commences at the beginning of the file.
     *
     * @param file         the file to open.
     * @param charset      the charset of the file.
     * @param deserializer the deserializer used to convert csv lines into objects.
     * @param <T>          the type of the objects to read.
     * @return A new {@code CsvReader} to read the specified file.
     * @throws IOException if an I/O error occurs.
     */
    <T> CsvReader<T> open(File file, Charset charset, CsvDeserializer<T> deserializer) throws IOException;

    /**
     * Opens a file for reading, returning a {@code CsvReader}. Bytes from the file are decoded into characters using
     * the specified charset. Reading commences at the beginning of the file.
     *
     * @param file         the file to open.
     * @param charset      the charset of the file.
     * @param options      reading options.
     * @param deserializer the deserializer used to convert csv lines into objects.
     * @param <T>          the type of the objects to read.
     * @return A new {@code CsvReader} to read the specified file.
     * @throws IOException if an I/O error occurs.
     */
    <T> CsvReader<T> open(File file, Charset charset, CsvOptions options, CsvDeserializer<T> deserializer) throws IOException;

    /**
     * Return a new {@code CsvReader} using the specified {@link Reader} for reading. Bytes from the file are decoded
     * into characters using the reader's charset. Reading commences at the point specified by the reader.
     *
     * @param reader       the {@link Reader} to read from.
     * @param deserializer the deserializer used to convert csv lines into objects.
     * @param <T>          the type of the objects to read.
     * @return A new {@code CsvReader} to read the specified file.
     */
    <T> CsvReader<T> open(Reader reader, CsvDeserializer<T> deserializer);

    /**
     * Return a new {@code CsvReader} using the specified {@link Reader} for reading. Bytes from the file are decoded
     * into characters using the reader's charset. Reading commences at the point specified by the reader.
     *
     * @param reader       the {@link Reader} to read from.
     * @param options      reading options.
     * @param deserializer the deserializer used to convert csv lines into objects.
     * @param <T>          the type of the objects to read.
     * @return A new {@code CsvReader} to read the specified file.
     */
    <T> CsvReader<T> open(Reader reader, CsvOptions options, CsvDeserializer<T> deserializer);
}
