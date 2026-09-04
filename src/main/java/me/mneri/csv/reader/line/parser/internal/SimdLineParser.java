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
import me.mneri.csv.reader.line.internal.InternalRecycledLine;

import java.io.IOException;

/**
 * Implementation of {@link LineParser} that leverages SIMD operations.
 */
public class SimdLineParser implements LineParser {
    private static final int EFB_TRAILING_ZEROES = Integer.numberOfTrailingZeros(Format.EFB);

    private final Format format;
    private final RandomAccessStream stream;

    private long bitmask;
    private long strideEnd;
    private long strideStart;

    public SimdLineParser(Format.Provider<?> provider, RandomAccessStream stream) {
        this.format = provider.provide();
        this.stream = stream;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        stream.close();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     * @throws CsvException {@inheritDoc}
     * @throws IOException  {@inheritDoc}
     */
    @Override
    public boolean next(InternalRecycledLine out) throws CsvException, IOException {
        int s = format.base();
        long bitmask = this.bitmask;
        long strideStart = this.strideStart;
        long strideEnd = this.strideEnd;

        out.reset();
        stream.compact(strideStart);
        do {
            long pos;
            int shift;

            // Optimization: The most frequent actions are to start and to end a field. For example, in a line with 5
            // fields there are 10 field-start/field-stop and only 1 end-of-line. We inserted a tighter loop , saving a
            // comparison per outer-loop iteration.
            do {
                // Calculate a bitmask with 1's set on the characters of interest and loop only on them.
                while (bitmask == 0L) {
                    strideStart = strideEnd;
                    strideEnd += Long.SIZE;
                    bitmask = format.bitmask(s, stream.array(strideStart, strideEnd), stream.index(strideStart));
                }
                shift = Long.numberOfTrailingZeros(bitmask);
                bitmask &= bitmask - 1L;
                pos = strideStart + shift;

                s = format.consumeSlow(s, stream.getChar(pos));

                if (isStartOfField(s)) {
                    out.startField(pos);
                }
                if (isEndOfField(s)) {
                    out.endField(pos - ((s & Format.EFB) >>> EFB_TRAILING_ZEROES));
                }
            } while (isJustStartOfFieldOrEndOfField(s));
            // Optimization: Dirty characters and replays are rare. Here we check for both with a single bitwise
            // operation, and if one of the bits is set we check again singularly.
            if (isPastDirtyOrReplay(s)) {
                if (isPastDirty(s)) {
                    out.dirty(pos - 1L);
                }
                if (isReplay(s)) {
                    bitmask |= 1L << shift;
                }
            }
        } while (isNotEndOfLineAndNotEndOfFileAndNotError(s));
        this.strideEnd = strideEnd;
        this.strideStart = strideStart;
        this.bitmask = bitmask;

        if (isNotEndOfFileAndNotError(s)) {
            return true;
        } else if (isError(s)) {
            unexpectedCharacterException();
        }
        return false;
    }

    private boolean isEndOfField(int s) {
        return (s & (Format.EFH | Format.EFB)) != 0;
    }

    private boolean isError(int s) {
        return (s & Format.ERH) != 0;
    }

    private boolean isJustStartOfFieldOrEndOfField(int s) {
        return (s & (Format.RMB | Format.RPL | Format.ELH | Format.ERH | Format.STP)) == 0;
    }

    private boolean isNotEndOfFileAndNotError(int s) {
        return (s & (Format.ERH | Format.STP)) == 0;
    }

    private boolean isNotEndOfLineAndNotEndOfFileAndNotError(int s) {
        return (s & (Format.ELH | Format.ERH | Format.STP)) == 0;
    }

    private boolean isPastDirty(int s) {
        return (s & Format.RMB) != 0;
    }

    private boolean isPastDirtyOrReplay(int s) {
        return (s & (Format.RMB | Format.RPL)) != 0;
    }

    private boolean isReplay(int s) {
        return (s & Format.RPL) != 0;
    }

    private boolean isStartOfField(int s) {
        return (s & Format.SFH) != 0;
    }

    private void unexpectedCharacterException() throws UnexpectedCharacterException {
        throw new UnexpectedCharacterException(0);
    }
}
