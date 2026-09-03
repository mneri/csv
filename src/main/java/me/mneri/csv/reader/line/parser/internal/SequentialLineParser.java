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

package me.mneri.csv.reader.line.parser.internal;

import me.mneri.csv.exception.CsvException;
import me.mneri.csv.exception.UnexpectedCharacterException;
import me.mneri.csv.format.Format;
import me.mneri.csv.io.internal.RandomAccessStream;
import me.mneri.csv.reader.line.internal.RecycledLineImpl;

import java.io.IOException;

import static me.mneri.csv.format.Format.*;

/**
 * Parse CSV lines processing one character after the other.
 */
public class SequentialLineParser implements LineParser {
    private static final int EFB_TRAILING_ZEROES = Integer.numberOfTrailingZeros(EFB);

    private final Format format;
    private final RecycledLineImpl line;
    private final RandomAccessStream reader;

    private int pos;

    public SequentialLineParser(RandomAccessStream reader, Format format, RecycledLineImpl line) {
        this.reader = reader;
        this.format = format;
        this.line = line;
    }

    public boolean parse() throws CsvException, IOException { // Bytecode size: 209 (OpenJDK 26)
        line.clear();
        reader.compact(pos);

        final Format format = this.format;
        int s = format.base();
        int pos = this.pos;
        do {
            // Optimization: The most frequent actions are to start and to end a field. For example, in a line with 5
            // fields there are 10 field-start/field-stop and only 1 end-of-line. We inserted a tighter loop on this,
            // saving a comparison per outer-loop.
            do {
                while (!isAny(s = format.consume(s, reader.getChar(pos)))) {
                    pos = pos + 1;
                }
                if (isStartOfField(s)) {
                    line.startField(pos);
                }
                if (isEndOfField(s)) {
                    line.endField(pos - ((s & EFB) >>> EFB_TRAILING_ZEROES));
                }
                pos = pos + 1;
            } while (isJustStartOfFieldOrEndOfField(s));
            // Optimization: Dirty characters and replays are rare. Here we check for both with a single bitwise
            // operation, and if one of the bits is set we check again singularly.
            if (isPastDirtyOrReplay(s)) {
                if (isPastDirty(s)) {
                    line.dirty(pos - 2); // -1: refers to previous position; -1: pos was already incremented
                }
                if (isReplay(s)) {
                    pos = pos - 2; // -1: go back one; -1: pos was already incremented
                }
            }
        } while (isNotEndOfLineAndNotEndOfFileAndNotError(s));
        this.pos = pos;

        if (isNotEndOfFileAndNotError(s)) {
            return true;
        } else if (isError(s)) {
            unexpectedCharacterException();
        }
        return false;
    }

    private boolean isAny(int s) {
        return (s & ANY) != 0;
    }

    private boolean isEndOfField(int s) {
        return (s & (EFH | EFB)) != 0;
    }

    private boolean isError(int s) {
        return (s & ERH) != 0;
    }

    private boolean isJustStartOfFieldOrEndOfField(int s) {
        return (s & (RMB | RPL | ELH | ERH | STP)) == 0;
    }

    private boolean isNotEndOfFileAndNotError(int s) {
        return (s & (ERH | STP)) == 0;
    }

    private boolean isNotEndOfLineAndNotEndOfFileAndNotError(int s) {
        return (s & (ELH | ERH | STP)) == 0;
    }

    private boolean isPastDirty(int s) {
        return (s & RMB) != 0;
    }

    private boolean isPastDirtyOrReplay(int s) {
        return (s & (RMB | RPL)) != 0;
    }

    private boolean isReplay(int s) {
        return (s & RPL) != 0;
    }

    private boolean isStartOfField(int s) {
        return (s & SFH) != 0;
    }

    private void unexpectedCharacterException() throws UnexpectedCharacterException {
        throw new UnexpectedCharacterException(0);
    }
}
