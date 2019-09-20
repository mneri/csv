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
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Spliterator.*;
import static java.util.Spliterators.spliteratorUnknownSize;

/**
 * Read csv streams and automatically transform lines into Java objects.
 *
 * @param <T> The type of the Java objects to read.
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public abstract class CsvReader<T> implements Closeable {
    //@formatter:off
    private static final int SOL =  0; // Start of line
    private static final int SOF =  6; // Start of field
    private static final int QOT = 12; // Quotation
    private static final int ESC = 18; // Escape
    private static final int TXT = 24; // Text
    private static final int CAR = 30; // Carriage return
    private static final int EOL = 36; // End of line
    private static final int EOF = 42; // End of file
    private static final int ERR = 48; // Error

    private static final int APP = 1 << 16; // Append
    private static final int MKF = 1 << 17; // Make field
    private static final int MKL = 1 << 18; // Make line

    private static final int[] ACTIONS = {
    //  *                "                ,                \r               \n               EOF
        TXT | APP      , QOT            , SOF | MKF      , CAR            , EOL | MKF | MKL, EOF            ,  // SOL
        TXT | APP      , QOT            , SOF | MKF      , CAR            , EOL | MKF | MKL, EOF | MKF | MKL,  // SOF
        QOT | APP      , ESC            , QOT | APP      , QOT | APP      , QOT | APP      , ERR            ,  // QOT
        ERR            , QOT | APP      , SOF | MKF      , CAR            , EOL | MKF | MKL, EOF | MKF | MKL,  // ESC
        TXT | APP      , TXT | APP      , SOF | MKF      , CAR            , EOL | MKF | MKL, EOF | MKF | MKL,  // TXT
        ERR            , ERR            , ERR            , ERR            , EOL | MKF | MKL, ERR            ,  // CAR
        ERR            , ERR            , ERR            , ERR            , ERR            , ERR            ,  // EOL
        ERR            , ERR            , ERR            , ERR            , ERR            , ERR            ,  // EOF
        ERR            , ERR            , ERR            , ERR            , ERR            , ERR            ,  // ERR
        0              , 0              , 0              , 0              , 0              , 0              ,
        0              , 0              , 0              , 0              , 0              , 0              ,
        0              , 0              , 0              , 0              , 0              , 0              };
    //@formatter:on

    private static final int STATE_MASK = 0x00_00_00_3f;

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private final char[] buffer;
    private final int delimiter;
    private int lines;
    private int next;
    private final int quotation;
    private final Reader reader;
    private int size;

    protected CsvReader(Reader reader, CsvOptions options) {
        this.reader = reader;

        buffer = new char[DEFAULT_BUFFER_SIZE];

        options.check();
        delimiter = options.getDelimiter();
        quotation = options.getQuotation();
    }

    private void append(RecyclableCsvLine line, char c) throws CsvLineTooBigException {
        try {
            line.append(c);
        } catch (ArrayIndexOutOfBoundsException ignored) {
            throw new CsvLineTooBigException(lines);
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
        reader.close();
    }

    private int columnOf(int c) {
        switch (c) {
            //@formatter:off
            case '\r': return 3;
            case '\n': return 4;
            case -1  : return 5; // EOF
            default  : if (c == delimiter) return 2;
                       if (c == quotation) return 1;
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
    public abstract boolean hasNext() throws CsvException, IOException;

    private static <T extends RecyclableCsvLine> Iterator<T> iterator(CsvReader<T> reader) {
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

    private void makeField(RecyclableCsvLine line) {
        line.markField();
    }

    /**
     * Return the next element in the reader.
     *
     * @return The next element.
     * @throws CsvException if the csv is not properly formatted.
     * @throws IOException  if an I/O error occurs.
     */
    public abstract T next() throws CsvException, IOException;

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
        return open(file, CsvOptions.defaultOptions(), deserializer);
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
        return open(file, TextUtil.defaultCharset(), options, deserializer);
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
        return open(file, charset, CsvOptions.defaultOptions(), deserializer);
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
        return open(new InputStreamReader(new FileInputStream(file), charset), options, deserializer);
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
        return open(reader, CsvOptions.defaultOptions(), deserializer);
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
        return new SequentialCsvReader<>(reader, options, deserializer);
    }

    protected boolean parseLine(RecyclableCsvLine line) throws CsvException, IOException {
        line.clear();

        int c;
        int row = SOL;

        do {
            c = nextChar();
            int action = ACTIONS[row + columnOf(c)];

            if ((action & APP) != 0) {
                append(line, (char) c);
            } else if ((action & MKF) != 0) {
                makeField(line);

                if ((action & MKL) != 0) {
                    lines++;
                    return true;
                }
            }

            row = action & STATE_MASK;
        } while (row < EOF);

        if (row == EOF) {
            return false;
        }

        throw new UnexpectedCharacterException(lines, c);
    }

    private int nextChar() throws IOException {
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
    public abstract void skip(int n) throws CsvException, IOException;

    protected boolean skipLines(int n) throws CsvException, IOException {
        int c;
        int row = SOL;
        int toSkip = n;

        do {
            c = nextChar();
            int transact = ACTIONS[row + columnOf(c)];

            if ((transact & MKL) != 0) {
                lines++;

                if (--toSkip == 0) {
                    return true;
                }

                row = SOL;
            } else {
                row = transact & STATE_MASK;
            }
        } while (row < EOF);

        if (row == EOF) {
            return false;
        }

        throw new UnexpectedCharacterException(lines, c);
    }

    /**
     * Opens a file for reading, returning a {@code CsvReader}. Bytes from the file are decoded into characters using
     * the default JVM charset. Reading commences at the beginning of the file.
     *
     * @param file         the file to open.
     * @param deserializer the deserializer used to convert csv lines into objects.
     * @param <T>          the type of the objects to read.
     * @return A new {@link Stream} of objects read from the specified file.
     * @throws IOException if an I/O error occurs.
     */
    public static <T> Stream<T> stream(File file, CsvDeserializer<T> deserializer, boolean parallel)
            throws IOException {
        return stream(file, CsvOptions.defaultOptions(), deserializer, parallel);
    }

    /**
     * Opens a file for reading, returning a {@code CsvReader}. Bytes from the file are decoded into characters using
     * the default JVM charset. Reading commences at the beginning of the file.
     *
     * @param file         the file to open.
     * @param options      reading options.
     * @param deserializer the deserializer used to convert csv lines into objects.
     * @param <T>          the type of the objects to read.
     * @return A new {@link Stream} of objects read from the specified file.
     * @throws IOException if an I/O error occurs.
     */
    public static <T> Stream<T> stream(File file, CsvOptions options, CsvDeserializer<T> deserializer,
                                       boolean parallel) throws IOException {
        return stream(file, TextUtil.defaultCharset(), options, deserializer, parallel);
    }

    /**
     * Opens a file for reading, returning a {@code CsvReader}. Bytes from the file are decoded into characters using
     * the specified charset. Reading commences at the beginning of the file.
     *
     * @param file         the file to open.
     * @param charset      the charset of the file.
     * @param deserializer the deserializer used to convert csv lines into objects.
     * @param <T>          the type of the objects to read.
     * @return A new {@link Stream} of objects read from the specified file.
     * @throws IOException if an I/O error occurs.
     */
    public static <T> Stream<T> stream(File file, Charset charset, CsvDeserializer<T> deserializer,
                                       boolean parallel) throws IOException {
        return stream(file, charset, CsvOptions.defaultOptions(), deserializer, parallel);
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
     * @return A new {@link Stream} of objects read from the specified file.
     * @throws IOException if an I/O error occurs.
     */
    public static <T> Stream<T> stream(File file, Charset charset, CsvOptions options, CsvDeserializer<T> deserializer,
                                       boolean parallel) throws IOException {
        return stream(new InputStreamReader(new FileInputStream(file), charset), options, deserializer, parallel);
    }

    /**
     * Return a new {@code CsvReader} using the specified {@link Reader} for reading. Bytes from the file are decoded
     * into characters using the reader's charset. Reading commences at the point specified by the reader.
     *
     * @param reader       the {@link Reader} to read from.
     * @param deserializer the deserializer used to convert csv lines into objects.
     * @param <T>          the type of the objects to read.
     * @return A new {@link Stream} of objects read from the specified file.
     */
    public static <T> Stream<T> stream(Reader reader, CsvDeserializer<T> deserializer, boolean parallel) {
        return stream(reader, CsvOptions.defaultOptions(), deserializer, parallel);
    }

    /**
     * Return a new {@code CsvReader} using the specified {@link Reader} for reading. Bytes from the file are decoded
     * into characters using the reader's charset. Reading commences at the point specified by the reader.
     *
     * @param reader       the {@link Reader} to read from.
     * @param options      reading options.
     * @param deserializer the deserializer used to convert csv lines into objects.
     * @param <T>          the type of the objects to read.
     * @return A new {@link Stream} of objects read from the specified file.
     */
    public static <T> Stream<T> stream(Reader reader, CsvOptions options, CsvDeserializer<T> deserializer,
                                       boolean parallel) {
        StreamableCsvReader csvReader = new StreamableCsvReader(reader, options);

        //@formatter:off
        return StreamSupport.stream(spliteratorUnknownSize(iterator(csvReader), IMMUTABLE | NONNULL | ORDERED), parallel)
                            .map(line -> {
                                try {
                                    return deserializer.deserialize(line);
                                } catch (Exception e) {
                                    throw new UncheckedCsvException(e);
                                }
                            })
                            .onClose(() -> {
                                try {
                                    csvReader.close();
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            });
        //@formatter:on
    }
}
