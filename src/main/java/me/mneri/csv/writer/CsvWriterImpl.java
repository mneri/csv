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

import me.mneri.csv.exception.CsvException;
import me.mneri.csv.option.CsvOptions;
import me.mneri.csv.exception.CsvConversionException;
import me.mneri.csv.exception.UncheckedCsvException;
import me.mneri.csv.serializer.CsvSerializer;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * Default implementation of {@link CsvWriter}.
 *
 * @param <T> The type of the Java objects to write.
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
class CsvWriterImpl<T> implements CsvWriter<T> {
    private static final int OPENED = 0;
    private static final int CLOSED = 1;

    private final int delimiter;
    private final List<String> line;
    private final int quotation;
    private final CsvSerializer<T> serializer;
    private int state = OPENED;
    private final Writer writer;

    CsvWriterImpl(Writer writer, CsvOptions options, CsvSerializer<T> serializer) {
        this.writer = writer;
        this.serializer = serializer;

        options.check();
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
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        if (state == CLOSED) {
            return;
        }

        state = CLOSED;
        line.clear();
        writer.flush();
        writer.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
        checkClosedState();
        writer.flush();
    }

    private boolean isQuotingNeeded(String string) {
        for (int i = 0; i < string.length(); i++) {
            int c = string.charAt(i);

            if (c == delimiter || c == quotation) {
                return true;
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(T object) throws CsvConversionException, IOException {
        checkClosedState();

        try {
            line.clear();
            serializer.serialize(object, line);
        } catch (Exception e) {
            throw new CsvConversionException(line, e);
        }

        writeLine();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeAll(Collection<T> objects) throws CsvConversionException, IOException {
        checkClosedState();

        for (T object : objects) {
            write(object);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeAll(Stream<T> stream) {
        checkClosedState();

        stream.forEach(object -> {
            try {
                write(object);
            } catch (CsvException e) {
                throw new UncheckedCsvException(e);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private void writeField(String string) throws IOException {
        if (string == null) {
            return;
        }

        if (isQuotingNeeded(string)) {
            writer.write(quotation);

            for (int i = 0; i < string.length(); i++) {
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

    private void writeLine() throws IOException {
        for (int i = 0; i < line.size(); i++) {
            writeField(line.get(i));

            if (i != line.size() - 1) {
                writer.write(delimiter);
            }
        }

        writer.write("\r\n");
    }
}
