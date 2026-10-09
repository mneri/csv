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
 * A page of characters read from the stream, and a table of the lines parsed from them.
 * <pre>
 * | complete lines | unfinished line | free space |
 * 0               end              limit       capacity
 * </pre>
 * A page goes round in four steps:
 * <ol>
 *     <li>{@code fill()} reads characters into the free space;</li>
 *     <li>the parser goes on from where it stopped, and records each line in the table as it ends;</li>
 *     <li>the client reads the complete lines;</li>
 *     <li>{@link #carryOverTo(Page)} moves the unfinished line to the start of the next page.</li>
 * </ol>
 * The lines are stored as a table, in three arrays that the parser fills from left to right:
 * <ul>
 *     <li>{@code fields}: a pair of positions in the buffer for each field, where it starts and where it ends. The
 *     start of the field in progress waits in {@code fields[fieldsSize]} until the field ends;</li>
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

    final char[] buffer;
    final int capacity;
    long base;  // The position of buf[0] in the stream, for error messages
    int end;    // Where the complete lines end and the unfinished line starts
    int fresh;  // Where the characters of the last fill() start: the parser goes on from here
    int limit;  // Where the characters read end

    final int[] fields;
    int fieldsSize;
    final int[] excluded;
    int excludedSize;
    final int[] lines; // Starts with (0, 0): where the first line starts
    private int lineCount;

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
        this.buffer = new char[capacity + PADDING];
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

    /**
     * Return {@code true} if the page holds as many characters as it can.
     *
     * @return {@code true} if the page is full.
     */
    boolean isFull() {
        return limit == capacity;
    }

    /**
     * Read characters into the free space: at least one, or the end of the stream, then more while the stream has them
     * ready, until the page is full. The page must not be full.
     */
    void fill(Reader in) throws IOException {
        fresh = limit;

        int read;
        do {
            read = in.read(buffer, limit, capacity - limit);
            if (read > 0) {
                limit += read;
            }
        } while (read >= 0 && limit < capacity && in.ready());

        last = read < 0;
        Arrays.fill(buffer, limit, limit + PADDING, '\0');
    }

    /**
     * Parsing failed: the error is thrown after the lines already in this page, and no page follows.
     *
     * @param t The error.
     */
    public void fail(Throwable t) {
        error = t;
        last = true;
    }

    void startField(int pos) {
        fields[fieldsSize] = pos;
    }

    void endField(int pos) {
        fields[fieldsSize + 1] = pos;
        fieldsSize += 2;
    }

    void exclude(int pos) {
        excluded[excludedSize++] = pos;
    }

    void endLine(int pos) { // The line ends before pos, where the next line starts
        lineCount++;
        lines[2 * lineCount] = fieldsSize;
        lines[2 * lineCount + 1] = excludedSize;
        end = pos;
    }

    /**
     * Move the unfinished line to the start of the next page, with the fields found in it so far and the characters to
     * exclude from them. The next page can be this page itself, and must be as large.
     * <pre>
     * this page | complete lines | unfinished line |
     *           0               end              limit
     * next page | unfinished line |
     *           0           limit - end
     * </pre>
     *
     * @param next The next page.
     */
    public void carryOverTo(Page next) {
        int shift = end; // The unfinished line moves back by this many characters

        // In the table, the entries of the unfinished line come after those of the complete lines
        int firstField = lines[2 * lineCount];
        int firstExcluded = lines[2 * lineCount + 1];

        System.arraycopy(buffer, shift, next.buffer, 0, limit - shift);
        copyPositions(fields, firstField, fieldsSize + 1, next.fields, shift); // + 1: the field in progress
        copyPositions(excluded, firstExcluded, excludedSize, next.excluded, shift);

        next.base = base + shift;
        next.end = 0;
        next.limit = limit - shift;
        next.fieldsSize = fieldsSize - firstField;
        next.excludedSize = excludedSize - firstExcluded;
        next.lineCount = 0;
        next.last = false;
        next.error = null;
    }

    // Copy the positions src[from, to) to the start of dst, moving them back by shift characters, like the characters
    private static void copyPositions(int[] src, int from, int to, int[] dst, int shift) {
        for (int i = from; i < to; i++) {
            dst[i - from] = src[i] - shift;
        }
    }
}
