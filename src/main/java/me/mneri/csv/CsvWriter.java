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

import me.mneri.csv.format.Format;
import me.mneri.csv.format.Rfc4180StrictFormat;
import me.mneri.csv.serializer.Serializer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Accept Java objects and serialize them into CSV streams.
 *
 * @param <T> The type of the Java objects to write.
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public class CsvWriter<T> implements Closeable, Flushable {
    private static final int OPEN = 0;
    private static final int CLOSED = 1;

    /**
     * Return a new {@link CsvWriter} in open state, writing to the specified file.
     *
     * @param file The file.
     * @param p    A provider of {@link Format}s.
     * @param ser  The serializer, mapping Java objects to CSV lines.
     * @param <T>  The type of object to map to a CSV line.
     * @return A new {@link CsvWriter}, in open state.
     * @throws IOException If an I/O error occurs.
     */
    public static <T> CsvWriter<T> open(File file, Format.Provider<? extends Format> p, Serializer<T> ser)
            throws IOException {
        return open(new FileWriter(file), p, ser);
    }

    /**
     * Return a new {@link CsvWriter} in open state, writing to the specified file in {@link Rfc4180StrictFormat}.
     *
     * @param file The file.
     * @param ser  The serializer, mapping Java objects to CSV lines.
     * @param <T>  The type of object to map to a CSV line.
     * @return A new {@link CsvWriter}, in open state.
     */
    public static <T> CsvWriter<T> open(File file, Serializer<T> ser) throws IOException {
        return open(file, Rfc4180StrictFormat.provider(), ser);
    }

    /**
     * Return a new {@link CsvWriter} in open state, writing to the specified writer.
     *
     * @param writer The writer.
     * @param p      A provider of {@link Format}s.
     * @param ser    The serializer, mapping Java objects to CSV lines.
     * @param <T>    The type of object to map to a CSV line.
     * @return A new {@link CsvWriter}, in open state.
     */
    public static <T> CsvWriter<T> open(Writer writer, Format.Provider<? extends Format> p, Serializer<T> ser) {
        return new CsvWriter<>(writer, p, ser);
    }

    /**
     * Return a new {@link CsvWriter} in open state, writing to the specified writer in {@link Rfc4180StrictFormat}.
     *
     * @param writer The writer.
     * @param ser    The serializer, mapping Java objects to CSV lines.
     * @param <T>    The type of object to map to a CSV line.
     * @return A new {@link CsvWriter}, in open state.
     */
    public static <T> CsvWriter<T> open(Writer writer, Serializer<T> ser) {
        return open(writer, Rfc4180StrictFormat.provider(), ser);
    }

    private final Format format;
    private final List<String> line;
    private final Serializer<T> ser;
    private int state = OPEN;
    private Writer writer;

    CsvWriter(Writer writer, Format.Provider<? extends Format> p, Serializer<T> ser) {
        this.writer = writer;
        this.format = p.provide();
        this.ser = ser;
        this.line = new ArrayList<>();
    }

    private void isOpenOrThrow() {
        if (state == CLOSED) {
            throw new IllegalStateException("The writer is closed.");
        }
    }

    @Override
    public void close() throws IOException {
        if (state == CLOSED) {
            return;
        }
        try {
            state = CLOSED;
            writer.flush();
            writer.close();
        } finally {
            writer = null;
        }
    }

    @Override
    public void flush() throws IOException {
        isOpenOrThrow();
        writer.flush();
    }

    private boolean isQuotingNeeded(String string) {
        for (int i = 0; i < string.length(); i++) {
            int c = string.charAt(i);
            if (c == format.delimiter() || c == format.qualifier() || c == '\r' || c == '\n') {
                return true;
            }
        }

        return false;
    }

    public void write(T object) throws IOException {
        isOpenOrThrow();
        line.clear();
        ser.serialize(object, line);
        writeLine(line);
    }

    public void writeAll(List<T> objects) throws IOException {
        isOpenOrThrow();
        for (T object : objects) {
            write(object);
        }
    }

    private void writeField(String string) throws IOException {
        if (string == null) {
            return;
        }

        int q = format.qualifier();
        if (isQuotingNeeded(string)) {
            writer.write(q);
            for (int i = 0; i < string.length(); i++) {
                int c = string.charAt(i);

                if (c == q) {
                    writer.write(q);
                    writer.write(q);
                } else {
                    writer.write(c);
                }
            }
            writer.write(q);
        } else {
            writer.write(string);
        }
    }

    private void writeLine(List<String> line) throws IOException {
        for (int i = 0; i < line.size() - 1; i++) {
            writeField(line.get(i));
            writer.write(format.delimiter());
        }
        writeField(line.get(line.size() - 1));
        writer.write("\r\n");
    }
}
