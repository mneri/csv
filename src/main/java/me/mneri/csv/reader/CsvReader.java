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

import me.mneri.csv.deserializer.Deserializer;
import me.mneri.csv.deserializer.StringListDeserializer;
import me.mneri.csv.extension.internal.Extensions;
import me.mneri.csv.format.Format;
import me.mneri.csv.format.Rfc4180FullyRelaxedFormat;
import me.mneri.csv.io.internal.RandomAccessCharStream;
import me.mneri.csv.io.internal.RandomAccessStream;
import me.mneri.csv.line.internal.InternalRecycledLine;
import me.mneri.csv.parser.internal.LineParser;
import me.mneri.csv.parser.internal.SequentialLineParser;
import me.mneri.csv.parser.internal.SimdLineParser;

import java.io.*;
import java.util.List;
import java.util.NoSuchElementException;

import static me.mneri.csv.format.Format.Provider;

/**
 * Read CSV streams and automatically transform lines into Java objects.
 *
 * @param <T> The type of the Java objects to read.
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public class CsvReader<T> implements AutoCloseable {
    private static final int ELEMENT_NOT_PREPARED = 0;
    private static final int ELEMENT_PREPARED = 1;
    private static final int NO_SUCH_ELEMENT = 2;
    private static final int READER_CLOSED = 3;

    private static final int MAX_LINE_SIZE = 65_536;

    /**
     * Return a new {@link CsvReader} in open state, reading from the specified file.
     *
     * @param f   The file.
     * @param p   A provider of {@link Format}s.
     * @param des The deserializer, mapping CSV lines to Java objects.
     * @param <T> The type of object a CSV line should be mapped to.
     * @return A new {@link CsvReader}, in open state.
     * @throws FileNotFoundException If the file does not exist.
     */
    public static <T> CsvReader<T> open(File f, Format.Provider<? extends Format> p, Deserializer<T> des)
            throws FileNotFoundException {
        return open(new FileReader(f), p, des);
    }

    /**
     * Return a new {@link CsvReader} in open state, reading from the specified file, deserializing each line into a
     * {@link List<String>}.
     *
     * @param f The file.
     * @param p A provider of {@link Format}s.
     * @return A new {@link CsvReader}, in open state.
     * @throws FileNotFoundException If the file does not exist.
     */
    public static CsvReader<List<String>> open(File f, Format.Provider<? extends Format> p) throws FileNotFoundException {
        return open(f, p, new StringListDeserializer());
    }

    /**
     * Return a new {@link CsvReader} in open state, reading from the specified file, parsing with
     * {@link Rfc4180FullyRelaxedFormat}.
     *
     * @param f   The file.
     * @param des The deserializer, mapping CSV lines to Java objects.
     * @param <T> The type of object a CSV line should be mapped to.
     * @return A new {@link CsvReader}, in open state.
     * @throws FileNotFoundException If the file does not exist.
     */
    public static <T> CsvReader<T> open(File f, Deserializer<T> des) throws FileNotFoundException {
        return open(f, Rfc4180FullyRelaxedFormat.provider(), des);
    }

    /**
     * Return a new {@link CsvReader} in open state, reading from the specified file, parsing with
     * {@link Rfc4180FullyRelaxedFormat}, deserializing each line into a {@link List<String>}.
     *
     * @param f The file.
     * @return A new {@link CsvReader}, in open state.
     * @throws FileNotFoundException If the file does not exist.
     */
    public static CsvReader<List<String>> open(File f) throws FileNotFoundException {
        return open(f, Rfc4180FullyRelaxedFormat.provider(), new StringListDeserializer());
    }

    /**
     * Return a new {@link CsvReader} in open state, reading from the specified reader.
     *
     * @param rdr The reader.
     * @param p   A provider of {@link Format}s.
     * @param des The deserializer, mapping CSV lines to Java objects.
     * @param <T> The type of object a CSV line should be mapped to.
     * @return A new {@link CsvReader}, in open state.
     */
    public static <T> CsvReader<T> open(Reader rdr, Format.Provider<? extends Format> p, Deserializer<T> des) {
        return new CsvReader<>(rdr, p, des);
    }

    /**
     * Return a new {@link CsvReader} in open state, reading from the specified reader, deserializing each line into a
     * {@link List<String>}.
     *
     * @param rdr The reader.
     * @param p   A provider of {@link Format}s.
     * @return A new {@link CsvReader}, in open state.
     */
    public static CsvReader<List<String>> open(Reader rdr, Format.Provider<? extends Format> p) {
        return open(rdr, p, new StringListDeserializer());
    }

    /**
     * Return a new {@link CsvReader} in open state, reading from the specified reader, parsing with
     * {@link Rfc4180FullyRelaxedFormat}.
     *
     * @param rdr The reader.
     * @param des The deserializer, mapping CSV lines to Java objects.
     * @param <T> The type of object a CSV line should be mapped to.
     * @return A new {@link CsvReader}, in open state.
     */
    public static <T> CsvReader<T> open(Reader rdr, Deserializer<T> des) {
        return open(rdr, Rfc4180FullyRelaxedFormat.provider(), des);
    }

    /**
     * Return a new {@link CsvReader} in open state, reading from the specified reader, deserializing each line into a
     * {@link List<String>}, parsing with {@link Rfc4180FullyRelaxedFormat}.
     *
     * @param rdr The reader.
     * @return A new {@link CsvReader}, in open state.
     */
    public static CsvReader<List<String>> open(Reader rdr) {
        return open(rdr, Rfc4180FullyRelaxedFormat.provider(), new StringListDeserializer());
    }

    private final Deserializer<T> deserializer;
    private final InternalRecycledLine out;
    private final LineParser parser;
    private int state = ELEMENT_NOT_PREPARED;

    private CsvReader(Reader reader, Provider<? extends Format> provider, Deserializer<T> deserializer) {
        this(new RandomAccessCharStream(reader, MAX_LINE_SIZE), provider, deserializer);
    }

    private CsvReader(RandomAccessStream reader, Provider<? extends Format> provider, Deserializer<T> deserializer) {
        this.out = new InternalRecycledLine(reader);
        this.deserializer = deserializer;

        if (Extensions.SIMD_SUPPORTED) {
            this.parser = new SimdLineParser(provider, reader);
        } else {
            this.parser = new SequentialLineParser(provider, reader);
        }
    }

    /**
     * Closes the stream and releases any system resources associated with it. Once the stream has been closed, further
     * {@link CsvReader#hasNext()}, {@link CsvReader#next()} invocations will throw an {@link IOException}. Closing a
     * previously closed stream has no effect.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        if (state == READER_CLOSED) {
            return;
        }
        state = READER_CLOSED;
        parser.close();
    }

    /**
     * Returns {@code true} if the reader has more elements (in other words, returns {@code true} if
     * {@link CsvReader#next()} would return an element rather than throwing an exception).
     *
     * @return {@code true} if the reader has more elements.
     * @throws IOException if an I/O error occurs.
     */
    public boolean hasNext() throws IOException { // Bytecode size: 36 (OpenJDK 26)
        // Optimization: In a typical hasNext()/next() loop, the state at the beginning of the call is always
        // ELEMENT_NOT PREPARED. We check if this is the case, and delegate the rest to the cold-path method hasNext2(),
        // reducing the bytecode size and making it more likely this method is inlined by the JIT compiler.
        if (state == ELEMENT_NOT_PREPARED) {
            if (parser.next(out)) {
                state = ELEMENT_PREPARED;
                return true;
            } else {
                state = NO_SUCH_ELEMENT;
            }
        }
        return hasNext2(); // Only called if the client doesn't follow the idiomatic pattern hasNext()/next()
    }

    public boolean hasNext2() {
        if (state == READER_CLOSED) {
            readerIsClosedException();
        }
        return state == ELEMENT_PREPARED;
    }

    /**
     * Return the next element in the reader.
     *
     * @return The next element.
     * @throws IOException If an I/O error occurs.
     */
    public T next() throws IOException { // Bytecode size: 38 (OpenJDK 26)
        // Optimization: In a typical hasNext()/next() loop, the state is already ELEMENT_PREPARED when this method is
        // called. We check this directly and delegate the rest to the cold-path method next2(), reducing the bytecode
        // size and making it more likely this method is inlined by the JIT compiler.
        if (state == ELEMENT_PREPARED) {
            state = ELEMENT_NOT_PREPARED;
            return deserializer.deserialize(out);
        }
        return next2(); // Only called if the client doesn't follow the idiomatic pattern hasNext()/next()
    }

    private T next2() throws IOException {
        if (state == READER_CLOSED) {
            readerIsClosedException();
        }
        if (!hasNext()) {
            noSuchElementException();
        }
        state = ELEMENT_NOT_PREPARED;
        return deserializer.deserialize(out);
    }

    private void noSuchElementException() {
        throw new NoSuchElementException();
    }

    private void readerIsClosedException() {
        throw new IllegalStateException("The reader is closed.");
    }
}
