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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Write Java objects as csv lines.
 *
 * @param <T> The type of the Java objects to write.
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public final class CsvWriter<T> implements Closeable, Flushable {
    private static final int OPENED = 0;
    private static final int CLOSED = 1;

    private final char delimiter;
    private List<String> line;
    private final char quotation;
    private CsvSerializer<T> serializer;
    private int state = OPENED;
    private Writer writer;

    private CsvWriter(Writer writer, CsvOptions options, CsvSerializer<T> serializer) {
        this.writer = writer;
        this.serializer = serializer;

        line = new ArrayList<>();

        delimiter = options.getDelimiter();
        quotation = options.getQuotation();
    }

    private void checkClosedState() {
        if (state == CLOSED) {
            throw new IllegalStateException("The writer is closed.");
        }
    }

    /**
     * Closes the stream, flushing it first. Once the stream has been closed, further {@link CsvWriter#flush()},
     * {@link CsvWriter#put(Object)}, {@link CsvWriter#putAll(Collection)} and {@link CsvWriter#putAll(Stream)}
     * invocations will cause an {@link IOException} to be thrown. Closing a previously closed stream has no effect.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        if (state == CLOSED) {
            return;
        }

        state = CLOSED;

        line = null;
        serializer = null;
        writer.flush();
        writer.close();
        writer = null;
    }

    private static CsvOptions defaultOptions() {
        return new CsvOptions();
    }

    /**
     * Flushes this stream by writing any buffered output to the underlying stream.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void flush() throws IOException {
        checkClosedState();
        writer.flush();
    }

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
    public static <T> CsvWriter<T> open(File file, Charset charset, CsvSerializer<T> serializer) throws IOException {
        return open(file, charset, defaultOptions(), serializer);
    }

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
    public static <T> CsvWriter<T> open(File file, Charset charset, CsvOptions options, CsvSerializer<T> serializer)
            throws IOException {
        return open(Files.newBufferedWriter(file.toPath(), charset), options, serializer);
    }

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
    public static <T> CsvWriter<T> open(File file, CsvSerializer<T> serializer) throws IOException {
        return open(file, defaultOptions(), serializer);
    }

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
    public static <T> CsvWriter<T> open(File file, CsvOptions options, CsvSerializer<T> serializer) throws IOException {
        return open(file, TextUtil.defaultCharset(), options, serializer);
    }

    /**
     * Return a new {@code CsvWriter} using the specified {@link Writer} for writing. Bytes file are encoded into
     * characters using the writer's charset. Writing commences at the point specified by the reader.
     *
     * @param writer     the {@link Writer} used to write.
     * @param serializer the serializer used to convert objects into csv lines.
     * @param <T>        the type of the objects to serialize.
     * @return A new {@code CsvWriter} to write into the specified file.
     */
    public static <T> CsvWriter<T> open(Writer writer, CsvSerializer<T> serializer) {
        return open(writer, defaultOptions(), serializer);
    }

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
    public static <T> CsvWriter<T> open(Writer writer, CsvOptions options, CsvSerializer<T> serializer) {
        return new CsvWriter<>(writer, options, serializer);
    }

    /**
     * Write a single object as csv line.
     *
     * @param object the object to write.
     * @throws CsvConversionException if an error occurs during object serialization.
     * @throws IOException            if an I/O error occurs.
     */
    public void put(T object) throws CsvConversionException, IOException {
        checkClosedState();

        try {
            line.clear();
            serializer.serialize(object, line);
        } catch (Exception e) {
            throw new CsvConversionException(line, e);
        }

        int fields = line.size();

        for (int i = 0; i < fields; i++) {
            putField(line.get(i));

            if (i != fields - 1) {
                writer.write(delimiter);
            }
        }

        writer.write("\r\n");
    }

    /**
     * Write a {@link Collection} of objects as csv lines.
     *
     * @param objects the objects to write.
     * @throws CsvConversionException if an error occurs during object serialization.
     * @throws IOException            if an I/O error occurs.
     */
    public void putAll(Collection<T> objects) throws CsvConversionException, IOException {
        checkClosedState();

        for (T object : objects) {
            put(object);
        }
    }

    /**
     * Write a {@link Stream} of objects as csv lines.
     *
     * @param stream the stream of objects to write.
     */
    public void putAll(Stream<T> stream) {
        checkClosedState();

        stream.forEach(object -> {
            try {
                put(object);
            } catch (CsvException e) {
                throw new UncheckedCsvException(e);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private void putField(String string) throws IOException {
        if (string == null) {
            return;
        }

        int length = string.length();
        boolean quote = false;

        if (string.length() == 0) {
            quote = true;
        } else {
            for (int i = 0; i < length; i++) {
                int c = string.charAt(i);

                if (c == delimiter || c == quotation) {
                    quote = true;
                    break;
                }
            }
        }

        if (quote) {
            writer.write(quotation);

            for (int i = 0; i < length; i++) {
                int c = string.charAt(i);

                if (c == quotation) {
                    writer.write(quotation);
                    writer.write(quotation);
                } else {
                    writer.write(c);
                }
            }

            writer.write(quotation);
        } else {
            writer.write(string);
        }
    }
}
