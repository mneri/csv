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

import me.mneri.csv.exception.CsvConversionException;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Stream;

/**
 * Write Java objects as csv lines.
 *
 * @param <T> The type of the Java objects to write.
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public interface CsvWriter<T> extends Closeable, Flushable {
    /**
     * Closes the stream, flushing it first. Once the stream has been closed, further {@link CsvWriter#flush()},
     * {@link CsvWriter#write(Object)}, {@link CsvWriter#writeAll(Collection)} and
     * {@link CsvWriter#writeAll(Stream)} invocations will cause an {@link IOException} to be thrown. Closing a
     * previously closed stream has no effect.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    void close() throws IOException;

    /**
     * Flushes this stream by writing any buffered output to the underlying stream.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    void flush() throws IOException;

    /**
     * Write a single object as csv line.
     *
     * @param object the object to write.
     * @throws CsvConversionException if an error occurs during object serialization.
     * @throws IOException            if an I/O error occurs.
     */
    void write(T object) throws CsvConversionException, IOException;

    /**
     * Write a {@link Collection} of objects as csv lines.
     *
     * @param objects the objects to write.
     * @throws CsvConversionException if an error occurs during object serialization.
     * @throws IOException            if an I/O error occurs.
     */
    void writeAll(Collection<T> objects) throws CsvConversionException, IOException;

    /**
     * Write a {@link Stream} of objects as csv lines.
     *
     * @param stream the stream of objects to write.
     */
    void writeAll(Stream<T> stream);
}
