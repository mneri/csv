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
import java.util.NoSuchElementException;

class SequentialCsvReader<T> extends CsvReader<T> {
    //@formatter:off
    private static final int ELEMENT_NOT_READ = 0;
    private static final int ELEMENT_READ     = 1;
    private static final int NO_SUCH_ELEMENT  = 2;
    private static final int CLOSED           = 3;
    //@formatter:on

    private final CsvDeserializer<T> deserializer;
    private final RecyclableCsvLine line;
    private int state = ELEMENT_NOT_READ;

    SequentialCsvReader(Reader reader, CsvOptions options, CsvDeserializer<T> deserializer) {
        super(reader, options);

        this.deserializer = deserializer;
        this.line = new RecyclableCsvLine(options.getMaxLineLength());
    }

    private void checkState() {
        if (state == CLOSED) {
            throw new IllegalStateException("The reader is closed.");
        }
    }

    @Override
    public void close() throws IOException {
        if (state == CLOSED) {
            return;
        }

        state = CLOSED;
        super.close();
    }

    @Override
    public boolean hasNext() throws CsvException, IOException {
        checkState();

        if (state == ELEMENT_READ) {
            return true;
        } else if (state == NO_SUCH_ELEMENT) {
            return false;
        }

        boolean read = parseLine(line);
        state = read ? ELEMENT_READ : NO_SUCH_ELEMENT;

        return read;
    }

    @Override
    public T next() throws CsvException, IOException {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        try {
            T element = deserializer.deserialize(line);
            state = ELEMENT_NOT_READ;
            return element;
        } catch (Exception e) {
            throw new CsvConversionException(line, e);
        }
    }

    @Override
    public void skip(int n) throws CsvException, IOException {
        checkState();

        if (state == NO_SUCH_ELEMENT) {
            return;
        }

        int toSkip = n;

        if (state == ELEMENT_READ) {
            state = ELEMENT_NOT_READ;

            if (--toSkip == 0) {
                return;
            }
        }

        state = skipLines(toSkip) ? ELEMENT_NOT_READ : NO_SUCH_ELEMENT;
    }
}
