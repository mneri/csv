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

import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorSpecies;
import me.mneri.csv.exception.CsvException;
import me.mneri.csv.exception.UnexpectedCharacterException;
import me.mneri.csv.format.Format;
import me.mneri.csv.io.internal.RandomAccessStream;
import me.mneri.csv.reader.line.internal.RecycledLineImpl;

import java.io.IOException;

import static me.mneri.csv.format.Format.*;

/**
 * Implementation of {@link LineParser} that leverages SIMD operations.
 */
public class SimdLineParser implements LineParser {
    private static final VectorSpecies<Short> SPECIES =
            ShortVector.SPECIES_PREFERRED.vectorBitSize() <= 512 ? ShortVector.SPECIES_PREFERRED : ShortVector.SPECIES_MAX;
    private static final int STRIDE = SPECIES.length();

    private static final int EFB_TRAILING_ZEROES = Integer.numberOfTrailingZeros(EFB);

    private final Format format;
    private final RecycledLineImpl line;
    private final RandomAccessStream stream;

    private long bitmask;
    private long strideEnd;
    private long pos;

    public SimdLineParser(RandomAccessStream stream, Format format, RecycledLineImpl line) {
        this.stream = stream;
        this.format = format;
        this.line = line;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CsvException {@inheritDoc}
     * @throws IOException  {@inheritDoc}
     */
    @Override
    public boolean parse() throws CsvException, IOException {
        int s = format.base();
        long bitmask = this.bitmask;
        long pos = this.pos;
        long strideEnd = this.strideEnd;

        line.clear();
        stream.compact(pos);
        do {
            // Optimization: The most frequent actions are to start and to end a field. For example, in a line with 5
            // fields there are 10 field-start/field-stop and only 1 end-of-line. We inserted a tighter loop , saving a
            // comparison per outer-loop iteration.
            do {
                do {
                    if (pos >= strideEnd) {
                        pos = strideEnd;
                        strideEnd += STRIDE;
                        bitmask = format.simd().bitmask(s, stream, pos, SPECIES);
                    }
                    int shift = Long.numberOfTrailingZeros(bitmask);
                    bitmask = (bitmask >>> shift) - 1;
                    pos += shift;
                } while (pos >= strideEnd);

                s = format.consumeSlow(s, stream.getChar(pos));

                if (isStartOfField(s)) {
                    line.startField(pos);
                }
                if (isEndOfField(s)) {
                    line.endField(pos - ((s & EFB) >>> EFB_TRAILING_ZEROES));
                }
            } while (isJustStartOfFieldOrEndOfField(s));
            // Optimization: Dirty characters and replays are rare. Here we check for both with a single bitwise
            // operation, and if one of the bits is set we check again singularly.
            if (isPastDirtyOrReplay(s)) {
                if (isPastDirty(s)) {
                    line.dirty(pos - 1);
                }
                if (isReplay(s)) {
                    bitmask |= 1L;
                }
            }
        } while (isNotEndOfLineAndNotEndOfFileAndNotError(s));
        this.strideEnd = strideEnd;
        this.bitmask = bitmask;
        this.pos = pos;

        if (isNotEndOfFileAndNotError(s)) {
            return true;
        } else if (isError(s)) {
            unexpectedCharacterException();
        }
        return false;
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
