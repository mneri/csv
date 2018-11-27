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

package me.mneri.csv;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utility methods for creating and manipulating streams.
 *
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public class CsvStreamSupport {
    private static <T> Iterator<T> iterator(CsvReader<T> reader) {
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                //@formatter:off
                try                    { return reader.hasNext(); }
                catch (CsvException e) { throw new UncheckedCsvException(e); }
                catch (IOException e)  { throw new UncheckedIOException(e); }
                //@formatter:on
            }

            @Override
            public T next() {
                //@formatter:off
                try                    { return reader.next(); }
                catch (CsvException e) { throw new UncheckedCsvException(e); }
                catch (IOException e)  { throw new UncheckedIOException(e); }
                //@formatter:on
            }
        };
    }

    private static <T> Spliterator<T> spliterator(CsvReader<T> reader) {
        int characteristics = Spliterator.IMMUTABLE | Spliterator.ORDERED;
        return Spliterators.spliteratorUnknownSize(iterator(reader), characteristics);
    }

    /**
     * Return a new {@link Stream} of objects using the specified {@link Reader} for reading. Bytes from the file are
     * decoded into characters using the reader's charset. Reading commences at the point specified by the reader.
     *
     * @param reader   the {@link CsvReader} to generate the stream from.
     * @param parallel if true then the returned stream is a parallel stream; if false the returned stream is a
     *                 sequential stream.
     * @param <T>      the type of the objects to read.
     * @return A new {@link Stream} of objects generated from the reader.
     */
    public static <T> Stream<T> stream(CsvReader<T> reader, boolean parallel) {
        //@formatter:off
        return StreamSupport.stream(spliterator(reader), parallel)
                            .onClose(() -> {
                                try {
                                    reader.close();
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            });
        //@formatter:on
    }
}
