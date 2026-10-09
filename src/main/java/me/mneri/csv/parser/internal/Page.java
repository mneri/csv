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

package me.mneri.csv.parser.internal;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

/**
 * A page of characters read from the stream, and a table of the lines parsed from it.
 * <p>
 * A page holds only complete lines. A line that doesn't fit at the end of a page (the tail) is carried to the start of
 * the next page, together with the fields found so far, and parsing resumes where it stopped.
 * <p>
 * The lines are stored as a table, in three arrays that the parser fills from left to right:
 * <ul>
 *     <li>{@code fields}: a pair of positions in the buffer for each field, where it starts and where it ends;</li>
 *     <li>{@code excluded}: the positions of the characters to drop from the fields, such as the first quote of an
 *     escaped quote;</li>
 *     <li>{@code lines}: where each line ends in the other two arrays. Line {@code i} owns the pairs in
 *     {@code fields[lines[2i], lines[2i + 2])} and the positions in {@code excluded[lines[2i + 1], lines[2i + 3])}.
 *     </li>
 * </ul>
 * The client reads the lines through a single {@link InternalRecycledLine}, which the page points at a line before
 * returning it.
 * <p>
 * The table is sized for the worst case (a field or a line for every character) when the page is created, so parsing
 * never checks for space. It must not: in our measurements, growing the table while the JIT profiles the parser made
 * parsing 50% slower for good, because the JIT compiled the allocation into the hot loop. Only the part of the table in
 * use takes space in the CPU caches.
 * <p>
 * A page never grows: a line longer than the page is an error.
 *
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public final class Page {
    // The SIMD parser always reads 64 characters at a time, even at the end of the page.
    private static final int PADDING = Long.SIZE;

    final char[] buf;
    final int capacity;
    long base;   // Absolute position of buf[0] in the stream, for error messages
    int limit;   // buf[0, limit) holds the characters read
    int tail;    // Start of the characters not parsed into lines, carried to the next page
    int parsed;  // buf[0, parsed) has already been parsed: parsing resumes here

    final int[] fields;
    int fieldsSize;
    final int[] excluded;
    int excludedSize;
    final int[] lines; // Starts with (0, 0): where the first line starts
    private int lineCount;
    private int lineStart;

    private final InternalRecycledLine line;
    private boolean last;
    private Throwable error;

    /**
     * Return a new {@code Page}.
     *
     * @param capacity The number of characters the page can hold.
     */
    public Page(int capacity) {
        this.capacity = capacity;
        this.buf = new char[capacity + PADDING];
        this.fields = new int[2 * (capacity + 1)];
        this.excluded = new int[capacity];
        this.lines = new int[2 * (capacity + 2)];
        this.line = new InternalRecycledLine(this);
    }

    /**
     * Return the number of lines in this page.
     *
     * @return The number of lines.
     */
    public int lineCount() {
        return lineCount;
    }

    /**
     * Return line {@code i}. The same object is returned for every line, and is valid until the next call.
     *
     * @param i The index of the line.
     * @return The line.
     */
    public InternalRecycledLine line(int i) {
        return line.moveTo(i);
    }

    /**
     * Return {@code true} if no page follows this one.
     *
     * @return {@code true} if no page follows this one.
     */
    public boolean isLast() {
        return last;
    }

    /**
     * Return {@code true} if an error stopped parsing after the last line of this page.
     *
     * @return {@code true} if an error stopped parsing after the last line of this page.
     */
    public boolean hasError() {
        return error != null;
    }

    /**
     * Throw the error that stopped parsing after the last line of this page. Call it only if {@link #hasError()} is
     * {@code true}.
     *
     * @throws IOException The error, or an {@code IOException} wrapping it if it's another checked exception.
     */
    public void rethrow() throws IOException {
        if (error instanceof IOException) {
            throw (IOException) error;
        }
        if (error instanceof RuntimeException) {
            throw (RuntimeException) error;
        }
        if (error instanceof Error) {
            throw (Error) error;
        }
        throw new IOException(error);
    }

    void startLine(int pos) {
        lineStart = pos;
    }

    void startField(int pos) {
        fields[fieldsSize] = pos;
    }

    void endField(int pos) {
        fields[fieldsSize + 1] = pos;
        fieldsSize += 2;
    }

    void dirty(int pos) {
        excluded[excludedSize++] = pos;
    }

    void endLine() {
        lineCount++;
        lines[2 * lineCount] = fieldsSize;
        lines[2 * lineCount + 1] = excludedSize;
    }

    /**
     * The line in progress ran into the end of the page: it will be carried over to the next page.
     */
    void stop() {
        tail = lineStart;
        parsed = limit; // If the page isn't full, the loader can read more and parsing resumes here
    }

    /**
     * The stream is over: no page follows this one.
     */
    void finish() {
        tail = limit;
    }

    /**
     * Parsing failed: the error is thrown after the lines already in this page.
     *
     * @param t The error.
     */
    public void fail(Throwable t) {
        error = t;
        last = true;
        finish();
    }

    /**
     * Carry the tail of this page over to the start of the next page, which is reset. The next page can be this page
     * itself, and must be as large.
     *
     * @param next The next page.
     */
    public void carryover(Page next) {
        int length = limit - tail;
        int fieldsFrom = lines[2 * lineCount];
        int excludedFrom = lines[2 * lineCount + 1];
        System.arraycopy(buf, tail, next.buf, 0, length);
        for (int i = fieldsFrom; i <= fieldsSize; i++) { // <=: the start of the field in progress, if any
            next.fields[i - fieldsFrom] = fields[i] - tail;
        }
        for (int i = excludedFrom; i < excludedSize; i++) {
            next.excluded[i - excludedFrom] = excluded[i] - tail;
        }
        next.base = base + tail;
        next.limit = length;
        next.tail = 0;
        next.parsed = length;
        next.fieldsSize = fieldsSize - fieldsFrom;
        next.excludedSize = excludedSize - excludedFrom;
        next.lineCount = 0;
        next.lineStart = 0;
        next.last = false;
        next.error = null;
    }

    /**
     * Return {@code true} if the page holds as many characters as it can.
     *
     * @return {@code true} if the page is full.
     */
    boolean isFull() {
        return limit == capacity;
    }

    /**
     * Read at least one character, or the end of the stream, then keep reading while the stream has characters ready,
     * until the page is full. The page must not be full.
     */
    void fill(Reader in) throws IOException {
        int read;
        do {
            read = in.read(buf, limit, capacity - limit);
            if (read > 0) {
                limit += read;
            }
        } while (read >= 0 && limit < capacity && in.ready());

        last = read < 0;
        Arrays.fill(buf, limit, limit + PADDING, '\0');
    }
}
