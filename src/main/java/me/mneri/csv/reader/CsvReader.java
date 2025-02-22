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

import static me.mneri.csv.format.Format.*;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Locale;
import java.util.NoSuchElementException;

import me.mneri.csv.deserializer.Deserializer;
import me.mneri.csv.exception.CsvConversionException;
import me.mneri.csv.exception.CsvException;
import me.mneri.csv.exception.LineTooLongException;
import me.mneri.csv.exception.UnexpectedCharacterException;
import me.mneri.csv.format.Format;
import me.mneri.csv.format.FormatProvider;
import me.mneri.csv.format.MsExcelFormat;
import me.mneri.csv.format.Rfc4180RelaxedFormat;

/**
 * Read csv streams and automatically transform lines into Java objects.
 *
 * @param <T> The type of the Java objects to read.
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public class CsvReader<T> implements Closeable {
    private static final int ELEMENT_NOT_PREPARED = 0;
    private static final int ELEMENT_PREPARED = 1;
    private static final int NO_SUCH_ELEMENT = 2;
    private static final int CLOSED = 3;

    private static final int MAX_LINE_SIZE = 32_768;
    private static final int MAX_READ_SIZE = 8_192;

    private final char[] buffer = new char[MAX_LINE_SIZE];
    private final Deserializer<T> deserializer;
    private final Format fmt;
    private final RecycledLineImpl line;
    private final Reader rdr;

    private int lines;
    private int mark;
    private int nextChar;
    private int offset;
    private int size;
    private int state = ELEMENT_NOT_PREPARED;

    /**
     * Return a new {@link CsvReader} in open state, reading from the specified file.
     *
     * @param file         The file.
     * @param provider     A provider of {@link Format}s.
     * @param deserializer The deserializer, mapping CSV lines to Java objects.
     * @param <T>          The type of object a CSV line should be mapped to.
     * @return A new {@link CsvReader}, in open state.
     * @throws FileNotFoundException If the file does not exist.
     */
    public static <T> CsvReader<T> open(File file, FormatProvider<?> provider, Deserializer<T> deserializer)
            throws FileNotFoundException {
        return open(new FileReader(file), provider, deserializer);
    }

    /**
     * Return a new {@link CsvReader} in open state, reading from the specified reader.
     *
     * @param rdr          The reader.
     * @param provider     A provider of {@link Format}s.
     * @param deserializer The deserializer, mapping CSV lines to Java objects.
     * @param <T>          The type of object a CSV line should be mapped to.
     * @return A new {@link CsvReader}, in open state.
     */
    public static <T> CsvReader<T> open(Reader rdr, FormatProvider<?> provider, Deserializer<T> deserializer) {
        return new CsvReader<>(rdr, provider, new RecycledLineImpl(), deserializer);
    }

    CsvReader(
            Reader rdr,
            FormatProvider<? extends Format> provider,
            RecycledLineImpl line,
            Deserializer<T> deserializer) {
        // A FormatProvider is used instead of a plain Format because Formats can be stateful. Reusing a stateful Format
        // across different CsvReader instances can cause parsing errors because the state, which was meant to be
        // private, would now be shared between different streams of data. If the client uses frameworks like Spring
        // that encourage injection and instance reuse, this error might become very hard to spot. A FormatProvider does
        // very little and might look like a waste, but could save clients hours of debugging.
        this.rdr = rdr;
        this.fmt = provider.provide();
        this.line = line;
        this.deserializer = deserializer;
    }

    /**
     * Closes the stream and releases any system resources associated with it. Once the stream has been closed, further
     * {@link CsvReader#hasNext()}, {@link CsvReader#next()} and {@link CsvReader#skip(int)} invocations will throw an
     * {@link IOException}. Closing a previously closed stream has no effect.
     *
     * @throws IOException if an I/O error occurs.
     */
    public void close() throws IOException {
        if (state == CLOSED) {
            return;
        }
        state = CLOSED;
        rdr.close();
    }

    /**
     * Return the next character in the reader stream.
     *
     * @return The character.
     * @throws LineTooLongException If a character can't be returned because the read buffer is full.
     * @throws IOException          if an I/O error occurs.
     */
    private int getNextChar() throws LineTooLongException, IOException {
        if (nextChar == size) {
            if (performRead() == -1) {
                return -1;
            }
        }
        return buffer[nextChar++];
    }

    /**
     * Return {@code true} if the state returned by the {@link Format} includes at least one of the specified flags.
     *
     * @param s     The state, as returned by {@link Format#base()} or {@link Format#consume(int, int)}.
     * @param flags The flags.
     * @return {@code true} if at least one of the flags are set, {@code false} otherwise.
     */
    static boolean isAnySet(int s, int flags) {
        return (s & flags) != 0;
    }

    /**
     * Return {@code true} if the state returned by the {@link Format} does not include any of the specified flags.
     *
     * @param s     The state, as returned by {@link Format#base()} or {@link Format#consume(int, int)}.
     * @param flags The flags.
     * @return {@code true} if none of the flags are set, {@code false} otherwise.
     */
    static boolean isNoneSet(int s, int flags) {
        return (s & flags) == 0;
    }

    /**
     * Returns {@code true} if the reader has more elements (in other words, returns {@code true} if
     * {@link CsvReader#next()} would return an element rather than throwing an exception).
     *
     * @return {@code true} if the reader has more elements.
     * @throws CsvException if the csv is not properly formatted.
     * @throws IOException  if an I/O error occurs.
     */
    public boolean hasNext() throws CsvException, IOException {
        //@formatter:off
        switch (state) {
            case ELEMENT_NOT_PREPARED: return prepareElement();
            case ELEMENT_PREPARED:     return true;
            case NO_SUCH_ELEMENT:      return false;
            case CLOSED:
            default:                   throw new IllegalStateException("The reader is closed.");
        }
        //@formatter:on
    }

    /**
     * Prepare and cache the next element.
     *
     * @return {@code true} if an element has been successfully read, {@code false} otherwise.
     * @throws CsvException if the csv is not properly formatted.
     * @throws IOException  if an I/O error occurs.
     */
    private boolean prepareElement() throws CsvException, IOException {
        boolean prepared = parseLine(fmt);
        state = prepared ? ELEMENT_PREPARED : NO_SUCH_ELEMENT;
        return prepared;
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
        try {
            state = ELEMENT_NOT_PREPARED;
            return deserializer.deserialize(line);
        } catch (Exception e) {
            throw new CsvConversionException(line, e);
        }
    }

    private boolean parseLine(Format fmt) throws CsvException, IOException {
        int s = fmt.base();
        int start = -1, length;

        line.reset();
        mark = nextChar;

        do {
            while (isNoneSet(s = fmt.consume(s, getNextChar()), ANY))
                ; // Intentionally empty

            if (isAnySet(s, SFH)) {
                start = (nextChar - 1) + offset;
            }
            if (isAnySet(s, EFH | EFB)) {
                length = nextChar + offset - (isAnySet(s, EFB) ? 2 : 1) - start;
                line.addField(new String(buffer, start - offset, length));
            }
            if (isAnySet(s, RLR)) {
                nextChar--;
            }
            if (isAnySet(s, RCB)) {
                shiftBuffer(start, start + 1, (nextChar - 2) - start);
                start++;
            }
        } while (isNoneSet(s, ELH | ERH | STP));

        lines++;

        if (isNoneSet(s, STP | ERH)) {
            return true;
        } else if (isAnySet(s, ERH)) {
            throw new UnexpectedCharacterException(lines, buffer[nextChar - 1]);
        } else {
            return false;
        }
    }

    private int performRead() throws IOException, LineTooLongException {
        if (buffer.length - size < MAX_READ_SIZE) {
            int length = size - mark;
            shiftBuffer(mark, 0, length);
            nextChar = size = length;
            offset += mark;
        }
        if (size == buffer.length) {
            throw new LineTooLongException(lines);
        }
        int read;
        if ((read = rdr.read(buffer, size, Math.min(MAX_READ_SIZE, buffer.length - size))) < 0) {
            return -1;
        }
        size += read;
        return 0;
    }

    private void shiftBuffer(int source, int dest, int length) {
        System.arraycopy(buffer, source, buffer, dest, length);
    }

    /**
     * Skip the next elements of the reader.
     *
     * @param n The number of elements to skip.
     * @throws CsvException if the csv is not properly formatted.
     * @throws IOException  if an I/O error occurs.
     */
    public void skip(int n) throws CsvException, IOException {
        int toSkip = n;
        switch (state) {
            case ELEMENT_PREPARED:
                state = ELEMENT_NOT_PREPARED;
                if (--toSkip == 0) {
                    return;
                }
            case NO_SUCH_ELEMENT:
                return;
            case CLOSED:
                throw new IllegalStateException("The reader is closed.");
        }
        state = skipLines(toSkip) ? ELEMENT_NOT_PREPARED : NO_SUCH_ELEMENT;
    }

    protected boolean skipLines(int n) throws CsvException, IOException {
        int s = fmt.base();
        mark = nextChar;
        int skipped = 0;

        do {
            while (isNoneSet(s = fmt.consume(s, getNextChar()), ELH | ERH | STP | RLR))
                ; // Intentionally empty

            if (isAnySet(s, RLR)) {
                nextChar--;
            }
        } while (++skipped <= n && isNoneSet(s, ERH | STP));

        lines += skipped;

        if (isNoneSet(s, STP | ERH)) {
            return true;
        } else if (isAnySet(s, ERH)) {
            throw new UnexpectedCharacterException(lines, buffer[nextChar - 1]);
        } else {
            return false;
        }
    }

    public static void main(String... args) throws IOException, CsvException {
        final int warmup = 16;
        for (int i = 0; i < warmup; i++) {
            execute();
        }
        System.out.println("-----");
        final int runs = 32;
        long total = 0;
        for (int i = 0; i < runs; i++) {
            total += execute();
        }
        System.out.println(total / runs);
    }

    private static long execute() throws IOException, CsvException {
        long blackhole = 0;
        File file = new File("/home/nerim313/Downloads/worldcitiespop.csv");
        Deserializer<Integer> deserializer = line -> {
            int hashCode = 0;
            int count = line.getFieldCount();
            for (int i = 0; i < count; i++) {
                String s = line.getString(i);
                hashCode ^= s != null ? s.hashCode() : 0;
            }
            return hashCode;
        };

        try (CsvReader<Integer> csv = CsvReader.open(file, new Rfc4180RelaxedFormat.Provider(), deserializer)) {
            long start = System.currentTimeMillis();
            while (csv.hasNext()) {
                blackhole ^= csv.next();
            }
            long end = System.currentTimeMillis();
            System.out.println((end - start) + " [" + blackhole + "]");
            return end - start;
        }
    }
}
