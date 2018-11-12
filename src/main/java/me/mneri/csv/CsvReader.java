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

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Read csv streams and automatically transform lines into Java objects.
 *
 * @param <T> The type of the Java objects to read.
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public final class CsvReader<T> implements Closeable {

    //@formatter:off
    private static final byte ERROR = -1;
    private static final byte START = 0;
    private static final byte QUOTE = 1;
    private static final byte ESCAP = 2;
    private static final byte STRNG = 3;
    private static final byte CARRG = 4;
    private static final byte FINSH = 5;

    private static final byte[][] TRANSITIONS = {
    //       *               "              ,              \r             \n             eof
            { STRNG        , QUOTE        , START        , CARRG        , FINSH        , FINSH         },  // START
            { QUOTE        , ESCAP        , QUOTE        , QUOTE        , QUOTE        , ERROR         },  // QUOTE
            { ERROR        , QUOTE        , START        , CARRG        , FINSH        , FINSH         },  // ESCAP
            { STRNG        , STRNG        , START        , CARRG        , FINSH        , FINSH         },  // STRNG
            { ERROR        , ERROR        , ERROR        , ERROR        , FINSH        , ERROR         },  // CARRG
            { ERROR        , ERROR        , ERROR        , ERROR        , ERROR        , ERROR         }}; // FINSH

    private static final byte NO_OP = 0;
    private static final byte ACCUM = 1;
    private static final byte FIELD = 2;
    private static final byte NLINE = 4;

    private static final byte[][] ACTIONS = {
    //       *              "              ,              \r             \n              eof
            { ACCUM        , NO_OP        , FIELD        , NO_OP        , FIELD | NLINE, NO_OP         },  // START
            { ACCUM        , NO_OP        , ACCUM        , ACCUM        , ACCUM        , NO_OP         },  // QUOTE
            { NO_OP        , ACCUM        , FIELD        , NO_OP        , FIELD | NLINE, FIELD | NLINE },  // ESCAP
            { ACCUM        , ACCUM        , FIELD        , NO_OP        , FIELD | NLINE, FIELD | NLINE },  // STRNG
            { NO_OP        , NO_OP        , NO_OP        , NO_OP        , FIELD | NLINE, NO_OP         },  // CARRG
            { NO_OP        , NO_OP        , NO_OP        , NO_OP        , NO_OP        , NO_OP         }}; // FINSH
    //@formatter:on

    //@formatter:off
    private static final int ELEMENT_NOT_READ = 0;
    private static final int ELEMENT_READ     = 1;
    private static final int NO_SUCH_ELEMENT  = 2;
    private static final int CLOSED           = 3;
    //@formatter:on

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private char[] buffer;
    private final char delimiter;
    private CsvDeserializer<T> deserializer;
    private T element;
    private RecyclableCsvLine line = new RecyclableCsvLine();
    private int lines;
    private int next;
    private final char quotation;
    private Reader reader;
    private int size;
    private int state = ELEMENT_NOT_READ;

    private CsvReader(Reader reader, CsvOptions options, CsvDeserializer<T> deserializer) {
        this.reader = reader;
        this.deserializer = deserializer;

        buffer = new char[DEFAULT_BUFFER_SIZE];

        delimiter = options.getDelimiter();
        quotation = options.getQuotation();
    }

    private void checkClosedState() {
        if (state == CLOSED) {
            throw new IllegalStateException("The reader is closed.");
        }
    }

    /**
     * Closes the stream and releases any system resources associated with it. Once the stream has been closed, further
     * {@link CsvReader#hasNext()}, {@link CsvReader#next()} and {@link CsvReader#skip(int)} invocations will throw an
     * {@link IOException}. Closing a previously closed stream has no effect.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        if (state == CLOSED) {
            return;
        }

        state = CLOSED;

        buffer = null;
        deserializer = null;
        element = null;
        line = null;
        reader.close();
        reader = null;
    }

    private static CsvOptions defaultOptions() {
        return new CsvOptions();
    }

    private int columnOf(int charCode) {
        switch (charCode) {
            //@formatter:off
            case '\r': return 3;
            case '\n': return 4;
            case -1  : return 5; // EOF
            default  :
                if (charCode == delimiter) return 2;
                if (charCode == quotation) return 1;

                return 0;
            //@formatter:on
        }
    }

    /**
     * Returns {@code true} if the reader has more elements. (In other words, returns {@code true} if
     * {@link CsvReader#next()} would return an element rather than throwing an exception).
     *
     * @return {@code true} if the reader has more elements.
     * @throws CsvException if the csv is not properly formatted.
     * @throws IOException  if an I/O error occurs.
     */
    public boolean hasNext() throws CsvException, IOException {
        //@formatter:off
        if      (state == ELEMENT_READ)    { return true; }
        else if (state == NO_SUCH_ELEMENT) { return false; }
        //@formatter:on

        checkClosedState();

        byte row = START;

        while (true) {
            int nextChar = read();
            int column = columnOf(nextChar);
            int action = ACTIONS[row][column];

            if ((action & ACCUM) != 0) {
                line.put(nextChar);
            } else if ((action & FIELD) != 0) {
                line.markField();

                if ((action & NLINE) != 0) {
                    lines++;

                    try {
                        T object = deserializer.deserialize(line);
                        line.clear();

                        element = object;
                        state = ELEMENT_READ;

                        return true;
                    } catch (Exception e) {
                        throw new CsvConversionException(line, e);
                    }
                }
            }

            row = TRANSITIONS[row][column];

            if (row == FINSH) {
                state = NO_SUCH_ELEMENT;
                return false;
            } else if (row == ERROR) {
                throw new UnexpectedCharacterException(lines, nextChar);
            }
        }
    }

    private Iterator<T> iterator() {
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                //@formatter:off
                try                    { return CsvReader.this.hasNext(); }
                catch (CsvException e) { throw new UncheckedCsvException(e); }
                catch (IOException e)  { throw new UncheckedIOException(e); }
                //@formatter:on
            }

            @Override
            public T next() {
                //@formatter:off
                try                    { return CsvReader.this.next(); }
                catch (CsvException e) { throw new UncheckedCsvException(e); }
                catch (IOException e)  { throw new UncheckedIOException(e); }
                //@formatter:on
            }
        };
    }

    /**
     * Return the next element in the reader.
     *
     * @return The next element.
     * @throws CsvException if the csv is not properly formatted.
     * @throws IOException  if an I/O error occurs.
     */
    public T next() throws CsvException, IOException {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        T result = element;
        element = null;
        state = ELEMENT_NOT_READ;

        return result;
    }

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
    public static <T> CsvReader<T> open(File file, CsvDeserializer<T> deserializer) throws IOException {
        return open(file, defaultOptions(), deserializer);
    }

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
    public static <T> CsvReader<T> open(File file, CsvOptions options, CsvDeserializer<T> deserializer)
            throws IOException {
        return open(file, Charset.defaultCharset(), options, deserializer);
    }

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
    public static <T> CsvReader<T> open(File file, Charset charset, CsvDeserializer<T> deserializer) throws IOException {
        return open(file, charset, defaultOptions(), deserializer);
    }

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
    public static <T> CsvReader<T> open(File file, Charset charset, CsvOptions options, CsvDeserializer<T> deserializer)
            throws IOException {
        return open(Files.newBufferedReader(file.toPath(), charset), options, deserializer);
    }

    /**
     * Return a new {@code CsvReader} using the specified {@link Reader} for reading. Bytes from the file are decoded
     * into characters using the reader's charset. Reading commences at the point specified by the reader.
     *
     * @param reader       the {@link Reader} to read from.
     * @param deserializer the deserializer used to convert csv lines into objects.
     * @param <T>          the type of the objects to read.
     * @return A new {@code CsvReader} to read the specified file.
     */
    public static <T> CsvReader<T> open(Reader reader, CsvDeserializer<T> deserializer) {
        return open(reader, defaultOptions(), deserializer);
    }

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
    public static <T> CsvReader<T> open(Reader reader, CsvOptions options, CsvDeserializer<T> deserializer) {
        return new CsvReader<>(reader, options, deserializer);
    }

    private int read() throws IOException {
        if (next >= size) {
            if ((size = reader.read(buffer, 0, buffer.length)) < 0) {
                return -1;
            }

            next = 0;
        }

        return buffer[next++];
    }

    /**
     * Skip the next elements of the reader.
     *
     * @param n The number of elements to skip.
     * @throws CsvException if the csv is not properly formatted.
     * @throws IOException  if an I/O error occurs.
     */
    public void skip(int n) throws CsvException, IOException {
        checkClosedState();

        if (state == NO_SUCH_ELEMENT) {
            return;
        }

        int toSkip = n;

        if (state == ELEMENT_READ) {
            element = null;
            state = ELEMENT_NOT_READ;

            if (--toSkip == 0) {
                return;
            }
        }

        byte row = START;

        while (true) {
            int nextChar = read();
            int column = columnOf(nextChar);
            int action = ACTIONS[row][column];

            if ((action & NLINE) != 0) {
                lines++;

                if (--toSkip == 0) {
                    return;
                }

                row = START;
            } else {
                row = TRANSITIONS[row][column];

                if (row == FINSH) {
                    state = NO_SUCH_ELEMENT;
                    return;
                } else if (row == ERROR) {
                    throw new UnexpectedCharacterException(lines, nextChar);
                }
            }
        }
    }

    private Spliterator<T> spliterator() {
        int characteristics = Spliterator.IMMUTABLE | Spliterator.ORDERED;
        return Spliterators.spliteratorUnknownSize(iterator(), characteristics);
    }

    /**
     * Opens a file for reading, returning a {@link Stream}. Bytes from the file are decoded into characters using
     * the default JVM charset. Reading commences at the beginning of the file.
     *
     * @param file         the file to open.
     * @param deserializer the deserializer used to convert csv lines into objects.
     * @param <T>          the type of the objects to read.
     * @return A new {@link Stream} of objects read from the specified file.
     * @throws IOException if an I/O error occurs.
     */
    public static <T> Stream<T> stream(File file, CsvDeserializer<T> deserializer) throws IOException {
        return stream(file, defaultOptions(), deserializer);
    }

    /**
     * Opens a file for reading, returning a {@link Stream}. Bytes from the file are decoded into characters using
     * the default JVM charset. Reading commences at the beginning of the file.
     *
     * @param file         the file to open.
     * @param options      reading options.
     * @param deserializer the deserializer used to convert csv lines into objects.
     * @param <T>          the type of the objects to read.
     * @return A new {@link Stream} of objects read from the specified file.
     * @throws IOException if an I/O error occurs.
     */
    public static <T> Stream<T> stream(File file, CsvOptions options, CsvDeserializer<T> deserializer)
            throws IOException {
        return stream(file, Charset.defaultCharset(), options, deserializer);
    }

    /**
     * Opens a file for reading, returning a {@link Stream}. Bytes from the file are decoded into characters using
     * the specified charset. Reading commences at the beginning of the file.
     *
     * @param file         the file to open.
     * @param charset      the charset of the file.
     * @param deserializer the deserializer used to convert csv lines into objects.
     * @param <T>          the type of the objects to read.
     * @return A new {@link Stream} of objects read from the specified file.
     * @throws IOException if an I/O error occurs.
     */
    public static <T> Stream<T> stream(File file, Charset charset, CsvDeserializer<T> deserializer) throws IOException {
        return stream(file, charset, defaultOptions(), deserializer);
    }

    /**
     * Opens a file for reading, returning a {@link Stream}. Bytes from the file are decoded into characters using
     * the specified charset. Reading commences at the beginning of the file.
     *
     * @param file         the file to open.
     * @param charset      the charset of the file.
     * @param options      reading options.
     * @param deserializer the deserializer used to convert csv lines into objects.
     * @param <T>          the type of the objects to read.
     * @return A new {@link Stream} of objects read from the specified file.
     * @throws IOException if an I/O error occurs.
     */
    public static <T> Stream<T> stream(File file, Charset charset, CsvOptions options, CsvDeserializer<T> deserializer)
            throws IOException {
        return stream(Files.newBufferedReader(file.toPath(), charset), options, deserializer);
    }

    /**
     * Return a new {@link Stream} of objects using the specified {@link Reader} for reading. Bytes from the file are
     * decoded into characters using the reader's charset. Reading commences at the point specified by the reader.
     *
     * @param reader       the {@link Reader} to read from.
     * @param deserializer the deserializer used to convert csv lines into objects.
     * @param <T>          the type of the objects to read.
     * @return A new {@code CsvReader} to read the specified file.
     */
    public static <T> Stream<T> stream(Reader reader, CsvDeserializer<T> deserializer) {
        return stream(reader, defaultOptions(), deserializer);
    }

    /**
     * Return a new {@link Stream} of objects using the specified {@link Reader} for reading. Bytes from the file are
     * decoded into characters using the reader's charset. Reading commences at the point specified by the reader.
     *
     * @param reader       the {@link Reader} to read from.
     * @param options      reading options.
     * @param deserializer the deserializer used to convert csv lines into objects.
     * @param <T>          the type of the objects to read.
     * @return A new {@code CsvReader} to read the specified file.
     */
    public static <T> Stream<T> stream(Reader reader, CsvOptions options, CsvDeserializer<T> deserializer) {
        //@formatter:off
        CsvReader<T> csvReader = CsvReader.open(reader, options, deserializer);
        return StreamSupport.stream(csvReader.spliterator(), false)
                            .onClose(() -> { try { csvReader.close(); } catch (Exception ignored) { } });
        //@formatter:on
    }
}
