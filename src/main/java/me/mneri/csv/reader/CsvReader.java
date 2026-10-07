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
import me.mneri.csv.format.Format;
import me.mneri.csv.format.Rfc4180FullyRelaxedFormat;
import me.mneri.csv.hint.Hint;
import me.mneri.csv.parser.internal.PageLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;

/**
 * Read CSV streams and automatically transform lines into Java objects.
 * <p>
 * To create a new instance of {@code CsvReader}, use one of the provided {@code open()} or {@code parallel()} factory
 * methods. The {@code parallel()} factory methods run on a background thread, created by the specified
 * {@link ThreadFactory}.
 * <p>
 * The reader supports various CSV dialects called formats, and they can be configured by passing a specific
 * {@code Format} provider as an argument. Different formats offer distinct interpretations of a CSV file: some enforce
 * strict adherence to the RFC 4180 standard, while others provide a more relaxed interpretation and are guaranteed
 * never to error (for example, {@link Rfc4180FullyRelaxedFormat}). Clients can choose the appropriate format based on
 * their specific use case.
 * <p>
 * Row-to-object conversion is handled by a {@link Deserializer} instance, allowing clients to supply their own custom
 * implementations.
 * <p>
 * <strong>Example</strong><br/>
 * <pre>{@code
 * Deserializer<Contact> deserializer = new ContactDeserializer();
 * try (CsvReader<Contact> reader = CsvReader.open(file, charset, Rfc4180StrictFormat.provider(), deserializer)) {
 *     while (reader.hasNext()) {
 *         Contact contact = reader.next();
 *         // ...
 *     }
 * }}</pre>
 *
 * @param <T> The type of the Java objects to read.
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 * @see Deserializer
 * @see Format
 */
public abstract class CsvReader<T> implements AutoCloseable {
    /**
     * Return a new {@link CsvReader} in open state, reading from the specified reader.
     *
     * @param rdr    The reader.
     * @param p      A provider of {@link Format}s.
     * @param des    The deserializer, mapping CSV lines to Java objects.
     * @param config The configuration.
     * @param <T>    The type of object a CSV line should be mapped to.
     * @return A new {@link CsvReader}, in open state.
     */
    public static <T> CsvReader<T> open(
            Reader rdr, Format.Provider<? extends Format> p, Deserializer<T> des, Configuration config) {
        return newInstance(null, rdr, p, des, config);
    }

    /**
     * Return a new {@link CsvReader} in open state, reading from the specified file.
     *
     * @param file    The file.
     * @param charset The charset of the file.
     * @param p       A provider of {@link Format}s.
     * @param des     The deserializer, mapping CSV lines to Java objects.
     * @param config  The configuration.
     * @param <T>     The type of object a CSV line should be mapped to.
     * @return A new {@link CsvReader}, in open state.
     * @throws IOException If the file can't be opened.
     */
    public static <T> CsvReader<T> open(
            File file, Charset charset, Format.Provider<? extends Format> p, Deserializer<T> des, Configuration config)
            throws IOException {
        return newInstance(null, new FileInputStream(file), charset, p, des, config);
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
        return open(rdr, p, des, Configuration.standard());
    }

    /**
     * Return a new {@link CsvReader} in open state, reading from the specified file.
     *
     * @param file    The file.
     * @param charset The charset of the file.
     * @param p       A provider of {@link Format}s.
     * @param des     The deserializer, mapping CSV lines to Java objects.
     * @param <T>     The type of object a CSV line should be mapped to.
     * @return A new {@link CsvReader}, in open state.
     * @throws IOException If the file can't be opened.
     */
    public static <T> CsvReader<T> open(
            File file, Charset charset, Format.Provider<? extends Format> p, Deserializer<T> des) throws IOException {
        return open(file, charset, p, des, Configuration.standard());
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
        return open(rdr, p, new StringListDeserializer(), Configuration.standard());
    }

    /**
     * Return a new {@link CsvReader} in open state, reading from the specified file, deserializing each line into a
     * {@link List<String>}.
     *
     * @param file    The file.
     * @param charset The charset of the file.
     * @param p       A provider of {@link Format}s.
     * @return A new {@link CsvReader}, in open state.
     * @throws IOException If the file can't be opened.
     */
    public static CsvReader<List<String>> open(File file, Charset charset, Format.Provider<? extends Format> p)
            throws IOException {
        return open(file, charset, p, new StringListDeserializer(), Configuration.standard());
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
        return open(rdr, Rfc4180FullyRelaxedFormat.provider(), des, Configuration.standard());
    }

    /**
     * Return a new {@link CsvReader} in open state, reading from the specified file, parsing with
     * {@link Rfc4180FullyRelaxedFormat}.
     *
     * @param file    The file.
     * @param charset The charset of the file.
     * @param des     The deserializer, mapping CSV lines to Java objects.
     * @param <T>     The type of object a CSV line should be mapped to.
     * @return A new {@link CsvReader}, in open state.
     * @throws IOException If the file can't be opened.
     */
    public static <T> CsvReader<T> open(File file, Charset charset, Deserializer<T> des) throws IOException {
        return open(file, charset, Rfc4180FullyRelaxedFormat.provider(), des, Configuration.standard());
    }

    /**
     * Return a new {@link CsvReader} in open state, reading from the specified reader, deserializing each line into a
     * {@link List<String>}, parsing with {@link Rfc4180FullyRelaxedFormat}.
     *
     * @param rdr The reader.
     * @return A new {@link CsvReader}, in open state.
     */
    public static CsvReader<List<String>> open(Reader rdr) {
        return open(rdr, Rfc4180FullyRelaxedFormat.provider(), new StringListDeserializer(), Configuration.standard());
    }

    /**
     * Return a new {@link CsvReader} in open state, reading from the specified file, deserializing each line into a
     * {@link List<String>}, parsing with {@link Rfc4180FullyRelaxedFormat}.
     *
     * @param file    The file.
     * @param charset The charset of the file.
     * @return A new {@link CsvReader}, in open state.
     * @throws IOException If the file can't be opened.
     */
    public static CsvReader<List<String>> open(File file, Charset charset) throws IOException {
        return open(file, charset, Rfc4180FullyRelaxedFormat.provider(), new StringListDeserializer(),
                Configuration.standard());
    }

    /**
     * Return a new parallel {@link CsvReader} in open state, reading from the specified reader. Pages are read and
     * parsed on a background thread created by the specified factory, while the calling thread deserializes lines.
     *
     * @param threads The factory of the background thread.
     * @param rdr     The reader.
     * @param p       A provider of {@link Format}s.
     * @param des     The deserializer, mapping CSV lines to Java objects.
     * @param config  The configuration.
     * @param <T>     The type of object a CSV line should be mapped to.
     * @return A new {@link CsvReader}, in open state.
     */
    public static <T> CsvReader<T> parallel(
            ThreadFactory threads, Reader rdr, Format.Provider<? extends Format> p, Deserializer<T> des,
            Configuration config) {
        return newInstance(Objects.requireNonNull(threads), rdr, p, des, config);
    }

    /**
     * Return a new parallel {@link CsvReader} in open state, reading from the specified file. Pages are read and
     * parsed on a background thread created by the specified factory, while the calling thread deserializes lines.
     *
     * @param threads The factory of the background thread.
     * @param file    The file.
     * @param charset The charset of the file.
     * @param p       A provider of {@link Format}s.
     * @param des     The deserializer, mapping CSV lines to Java objects.
     * @param config  The configuration.
     * @param <T>     The type of object a CSV line should be mapped to.
     * @return A new {@link CsvReader}, in open state.
     * @throws IOException If the file can't be opened.
     */
    public static <T> CsvReader<T> parallel(
            ThreadFactory threads, File file, Charset charset, Format.Provider<? extends Format> p, Deserializer<T> des,
            Configuration config) throws IOException {
        return newInstance(Objects.requireNonNull(threads), new FileInputStream(file), charset, p, des, config);
    }

    /**
     * Return a new parallel {@link CsvReader} in open state, reading from the specified reader.
     *
     * @param threads The factory of the background thread.
     * @param rdr     The reader.
     * @param p       A provider of {@link Format}s.
     * @param des     The deserializer, mapping CSV lines to Java objects.
     * @param <T>     The type of object a CSV line should be mapped to.
     * @return A new {@link CsvReader}, in open state.
     */
    public static <T> CsvReader<T> parallel(
            ThreadFactory threads, Reader rdr, Format.Provider<? extends Format> p, Deserializer<T> des) {
        return parallel(threads, rdr, p, des, Configuration.standard());
    }

    /**
     * Return a new parallel {@link CsvReader} in open state, reading from the specified file.
     *
     * @param threads The factory of the background thread.
     * @param file    The file.
     * @param charset The charset of the file.
     * @param p       A provider of {@link Format}s.
     * @param des     The deserializer, mapping CSV lines to Java objects.
     * @param <T>     The type of object a CSV line should be mapped to.
     * @return A new {@link CsvReader}, in open state.
     * @throws IOException If the file can't be opened.
     */
    public static <T> CsvReader<T> parallel(
            ThreadFactory threads, File file, Charset charset, Format.Provider<? extends Format> p, Deserializer<T> des)
            throws IOException {
        return parallel(threads, file, charset, p, des, Configuration.standard());
    }

    /**
     * Return a new parallel {@link CsvReader} in open state, reading from the specified reader, deserializing each
     * line into a {@link List<String>}.
     *
     * @param threads The factory of the background thread.
     * @param rdr     The reader.
     * @param p       A provider of {@link Format}s.
     * @return A new {@link CsvReader}, in open state.
     */
    public static CsvReader<List<String>> parallel(
            ThreadFactory threads, Reader rdr, Format.Provider<? extends Format> p) {
        return parallel(threads, rdr, p, new StringListDeserializer(), Configuration.standard());
    }

    /**
     * Return a new parallel {@link CsvReader} in open state, reading from the specified file, deserializing each line
     * into a {@link List<String>}.
     *
     * @param threads The factory of the background thread.
     * @param file    The file.
     * @param charset The charset of the file.
     * @param p       A provider of {@link Format}s.
     * @return A new {@link CsvReader}, in open state.
     * @throws IOException If the file can't be opened.
     */
    public static CsvReader<List<String>> parallel(
            ThreadFactory threads, File file, Charset charset, Format.Provider<? extends Format> p) throws IOException {
        return parallel(threads, file, charset, p, new StringListDeserializer(), Configuration.standard());
    }

    /**
     * Return a new parallel {@link CsvReader} in open state, reading from the specified reader, parsing with
     * {@link Rfc4180FullyRelaxedFormat}.
     *
     * @param threads The factory of the background thread.
     * @param rdr     The reader.
     * @param des     The deserializer, mapping CSV lines to Java objects.
     * @param <T>     The type of object a CSV line should be mapped to.
     * @return A new {@link CsvReader}, in open state.
     */
    public static <T> CsvReader<T> parallel(ThreadFactory threads, Reader rdr, Deserializer<T> des) {
        return parallel(threads, rdr, Rfc4180FullyRelaxedFormat.provider(), des, Configuration.standard());
    }

    /**
     * Return a new parallel {@link CsvReader} in open state, reading from the specified file, parsing with
     * {@link Rfc4180FullyRelaxedFormat}.
     *
     * @param threads The factory of the background thread.
     * @param file    The file.
     * @param charset The charset of the file.
     * @param des     The deserializer, mapping CSV lines to Java objects.
     * @param <T>     The type of object a CSV line should be mapped to.
     * @return A new {@link CsvReader}, in open state.
     * @throws IOException If the file can't be opened.
     */
    public static <T> CsvReader<T> parallel(ThreadFactory threads, File file, Charset charset, Deserializer<T> des)
            throws IOException {
        return parallel(threads, file, charset, Rfc4180FullyRelaxedFormat.provider(), des, Configuration.standard());
    }

    /**
     * Return a new parallel {@link CsvReader} in open state, reading from the specified reader, deserializing each
     * line into a {@link List<String>}, parsing with {@link Rfc4180FullyRelaxedFormat}.
     *
     * @param threads The factory of the background thread.
     * @param rdr     The reader.
     * @return A new {@link CsvReader}, in open state.
     */
    public static CsvReader<List<String>> parallel(ThreadFactory threads, Reader rdr) {
        return parallel(threads, rdr, Rfc4180FullyRelaxedFormat.provider(), new StringListDeserializer(),
                Configuration.standard());
    }

    /**
     * Return a new parallel {@link CsvReader} in open state, reading from the specified file, deserializing each line
     * into a {@link List<String>}, parsing with {@link Rfc4180FullyRelaxedFormat}.
     *
     * @param threads The factory of the background thread.
     * @param file    The file.
     * @param charset The charset of the file.
     * @return A new {@link CsvReader}, in open state.
     * @throws IOException If the file can't be opened.
     */
    public static CsvReader<List<String>> parallel(ThreadFactory threads, File file, Charset charset)
            throws IOException {
        return parallel(threads, file, charset, Rfc4180FullyRelaxedFormat.provider(), new StringListDeserializer(),
                Configuration.standard());
    }

    private static <T> CsvReader<T> newInstance(ThreadFactory threads, Reader in, Format.Provider<? extends Format> p, Deserializer<T> des, Configuration config) {
        PageLoader loader = new PageLoader(in, p, config.hints());
        if (threads == null) {
            return new SequentialCsvReader<>(config.maxLineSize(), loader, des);
        }
        return new ParallelCsvReader<>(threads, config.maxLineSize(), loader, des);
    }

    static <T> CsvReader<T> newInstance(ThreadFactory threads, InputStream in, Charset charset, Format.Provider<? extends Format> p, Deserializer<T> des, Configuration config)
        throws IOException {
        try {
            return newInstance(threads, new InputStreamReader(in, charset), p, des, config);
        } catch (Throwable e) {
            in.close();
            throw e;
        }
    }

    CsvReader() {
    }

    /**
     * Closes the stream and releases any system resources associated with it. Once the reader has been closed, further
     * {@link CsvReader#hasNext()}, {@link CsvReader#next()} invocations will throw an {@link IllegalStateException}.
     * Closing a previously closed reader has no effect.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public abstract void close() throws IOException;

    /**
     * Returns {@code true} if the reader has more elements (in other words, returns {@code true} if
     * {@link CsvReader#next()} would return an element rather than throwing an exception).
     *
     * @return {@code true} if the reader has more elements.
     * @throws IOException if an I/O error occurs.
     */
    public abstract boolean hasNext() throws IOException;

    /**
     * Return the next element in the reader.
     *
     * @return The next element.
     * @throws IOException If an I/O error occurs.
     */
    public abstract T next() throws IOException;

    /**
     * The settings of a {@link CsvReader}.
     * <p>
     * A configuration is created with a {@link Builder}, starting from the defaults:
     * <pre>{@code
     * Configuration config = Configuration.builder()
     *         .withMaxLineSize(32_768)
     *         .withHints(Hint.TINY_FIELDS)
     *         .build();
     * }</pre>
     *
     * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
     */
    public static final class Configuration {
        /**
         * The default maximum length of a line, in characters.
         */
        public static final int DEFAULT_MAX_LINE_SIZE = 4_096;

        /**
         * The largest maximum length of a line, in characters. A page holds arrays of about twice as many elements,
         * and their length must fit an {@code int}.
         */
        public static final int MAX_LINE_SIZE_LIMIT = 1 << 29;

        /**
         * The default hints to the reader: none.
         */
        public static final long DEFAULT_HINTS = 0L;

        private int maxLineSize = DEFAULT_MAX_LINE_SIZE;
        private long hints = DEFAULT_HINTS;

        private Configuration() {
        }

        /**
         * Return a new configuration with the default settings.
         *
         * @return A new configuration.
         */
        public static Configuration standard() {
            return new Configuration();
        }

        /**
         * Return a new builder, starting from the default settings.
         *
         * @return A new builder.
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Return the maximum length of a line, in characters, line break included.
         *
         * @return The maximum length of a line.
         */
        public int maxLineSize() {
            return maxLineSize;
        }

        /**
         * Return the hints to the reader.
         *
         * @return The hints.
         * @see Hint
         */
        public long hints() {
            return hints;
        }

        /**
         * A builder of {@link Configuration}s. A builder builds a single configuration: once {@link #build()} is
         * called, the builder can't be used anymore.
         */
        public static final class Builder {
            private Configuration configuration = new Configuration();

            private Builder() {
            }

            /**
             * Set the maximum length of a line.
             * <p>
             * The reader reads the stream a page at a time, and a page is as large as the longest line it accepts: a
             * longer line is an error. There must be a limit, or the reader would be subject to
             * {@link OutOfMemoryError} attacks.
             * <p>
             * Small pages are faster, because they stay in the CPU caches. A page takes about 22 bytes per character.
             *
             * @param maxLineSize The maximum length of a line, in characters, line break included.
             * @return This builder.
             * @throws IllegalArgumentException If the length is not between 1 and
             *                                  {@link Configuration#MAX_LINE_SIZE_LIMIT}.
             * @throws IllegalStateException    If the configuration has already been built.
             */
            public Builder withMaxLineSize(int maxLineSize) {
                notBuiltOrThrow();
                if (maxLineSize <= 0 || maxLineSize > MAX_LINE_SIZE_LIMIT) {
                    throw new IllegalArgumentException(
                            "The maximum line size must be between 1 and " + MAX_LINE_SIZE_LIMIT + ": " + maxLineSize);
                }
                configuration.maxLineSize = maxLineSize;
                return this;
            }

            /**
             * Set the hints to the reader.
             *
             * @param hints The hints to the reader.
             * @return This builder.
             * @throws IllegalStateException If the configuration has already been built.
             * @see Hint
             */
            public Builder withHints(long hints) {
                notBuiltOrThrow();
                configuration.hints = hints;
                return this;
            }

            /**
             * Return the configuration.
             *
             * @return The configuration.
             * @throws IllegalStateException If the configuration has already been built.
             */
            public Configuration build() {
                notBuiltOrThrow();
                Configuration built = configuration;
                configuration = null;
                return built;
            }

            private void notBuiltOrThrow() {
                if (configuration == null) {
                    throw new IllegalStateException("The configuration has already been built.");
                }
            }
        }
    }
}
