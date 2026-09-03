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
import me.mneri.csv.serializer.Serializer;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Default implementation of {@link CsvWriter}.
 *
 * @param <T> The type of the Java objects to write.
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public class CsvWriter<T> implements Closeable, Flushable {
    private static final int OPENED = 0;
    private static final int CLOSED = 1;

    private final int del;
    private final List<String> line;
    private final int qual;
    private final Serializer<T> ser;
    private int state = OPENED;
    private final Writer wtr;

    CsvWriter(Writer wtr, Serializer<T> ser) {
        this.wtr = wtr;
        this.ser = ser;

        line = new ArrayList<>();
        del = ',';
        qual = '"';
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
        state = CLOSED;
        line.clear();
        wtr.flush();
        wtr.close();
    }

    @Override
    public void flush() throws IOException {
        isOpenOrThrow();
        wtr.flush();
    }

    private boolean isQuotingNeeded(String string) {
        for (int i = 0; i < string.length(); i++) {
            int c = string.charAt(i);

            if (c == del || c == qual) {
                return true;
            }
        }

        return false;
    }

    public void write(T object) throws CsvConversionException, IOException {
        isOpenOrThrow();

        try {
            line.clear();
            ser.serialize(object, line);
        } catch (Exception e) {
            throw new CsvConversionException(line, e);
        }

        writeLine();
    }

    public void writeAll(Collection<T> objects) throws CsvConversionException, IOException {
        isOpenOrThrow();

        for (T object : objects) {
            write(object);
        }
    }

    private void writeField(String string) throws IOException {
        if (string == null) {
            return;
        }

        if (isQuotingNeeded(string)) {
            wtr.write(qual);

            for (int i = 0; i < string.length(); i++) {
                int c = string.charAt(i);

                if (c == qual) {
                    wtr.write(qual);
                    wtr.write(qual);
                } else {
                    wtr.write(c);
                }
            }

            wtr.write(qual);
        } else {
            wtr.write(string);
        }
    }

    private void writeLine() throws IOException {
        for (int i = 0; i < line.size(); i++) {
            writeField(line.get(i));

            if (i != line.size() - 1) {
                wtr.write(del);
            }
        }

        wtr.write("\r\n");
    }
}
