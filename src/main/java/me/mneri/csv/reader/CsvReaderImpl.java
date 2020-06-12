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

import me.mneri.csv.deserializer.CsvDeserializer;
import me.mneri.csv.exception.CsvConversionException;
import me.mneri.csv.exception.CsvException;
import me.mneri.csv.exception.CsvLineTooBigException;
import me.mneri.csv.exception.UnexpectedCharacterException;
import me.mneri.csv.option.CsvOptions;

import java.io.IOException;
import java.io.Reader;
import java.util.NoSuchElementException;

/**
 * Default implementation of {@link CsvReader}.
 *
 * @param <T> The type of the Java objects to read.
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
class CsvReaderImpl<T> implements CsvReader<T> {
    //@formatter:off
    private static final int SOL = 0;  // Start of line
    private static final int SOF = 6;  // Start of field
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
            TXT | APP,           QOT,             SOF | MKF,       CAR,             EOL | MKF | MKL, EOF,              // SOL
            TXT | APP,           QOT,             SOF | MKF,       CAR,             EOL | MKF | MKL, EOF | MKF | MKL,  // SOF
            QOT | APP,           ESC,             QOT | APP,       QOT | APP,       QOT | APP,       ERR,              // QOT
            ERR,                 QOT | APP,       SOF | MKF,       CAR,             EOL | MKF | MKL, EOF | MKF | MKL,  // ESC
            TXT | APP,           TXT | APP,       SOF | MKF,       CAR,             EOL | MKF | MKL, EOF | MKF | MKL,  // TXT
            ERR,                 ERR,             ERR,             ERR,             EOL | MKF | MKL, ERR,              // CAR
            ERR,                 ERR,             ERR,             ERR,             ERR,             ERR,              // EOL
            ERR,                 ERR,             ERR,             ERR,             ERR,             ERR,              // EOF
            ERR,                 ERR,             ERR,             ERR,             ERR,             ERR,              // ERR
            0,                   0,               0,               0,               0,               0,
            0,                   0,               0,               0,               0,               0,
            0,                   0,               0,               0,               0,               0};

    private static final int STATE_MASK = 0x00_00_00_3f;

    private static final int ELEMENT_NOT_READ = 0;
    private static final int ELEMENT_READ = 1;
    private static final int NO_SUCH_ELEMENT = 2;
    private static final int CLOSED = 3;
    //@formatter:on

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    private final char[] buffer;
    private final int delimiter;
    private final CsvDeserializer<T> deserializer;
    private final RecyclableCsvLineImpl line;
    private int lines;
    private int next;
    private final int quotation;
    private final Reader reader;
    private int size;
    private int state = ELEMENT_NOT_READ;

    CsvReaderImpl(Reader reader, CsvOptions options, CsvDeserializer<T> deserializer) {
        this.reader = reader;
        this.deserializer = deserializer;

        buffer = new char[DEFAULT_BUFFER_SIZE];

        options.check();
        delimiter = options.getDelimiter();
        line = new RecyclableCsvLineImpl(options.getMaxLineLength());
        quotation = options.getQuotation();
    }

    private void append(RecyclableCsvLineImpl line, char c) throws CsvLineTooBigException {
        try {
            line.append(c);
        } catch (ArrayIndexOutOfBoundsException ignored) {
            throw new CsvLineTooBigException(lines);
        }
    }

    private void checkState() {
        if (state == CLOSED) {
            throw new IllegalStateException("The reader is closed.");
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
        reader.close();
    }

    private int columnOf(int c) {
        switch (c) {
            case '\r': return 3;
            case '\n': return 4;
            case -1:   return 5; // EOF
            default:
                if (c == delimiter) return 2;
                if (c == quotation) return 1;
                return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
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

    private void makeField(RecyclableCsvLineImpl line) {
        line.markField();
    }

    /**
     * {@inheritDoc}
     */
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

    protected boolean parseLine(RecyclableCsvLineImpl line) throws CsvException, IOException {
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
     * {@inheritDoc}
     */
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
}
