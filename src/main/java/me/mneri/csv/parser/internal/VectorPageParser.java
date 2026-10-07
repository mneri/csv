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

import me.mneri.csv.exception.UnexpectedCharacterException;
import me.mneri.csv.format.Format;

import static me.mneri.csv.format.Format.*;

/**
 * A {@link PageParser} that uses SIMD instructions to skip the characters the format doesn't need to see.
 *
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public final class VectorPageParser implements PageParser {
    private static final int EFB_TRAILING_ZEROES = Integer.numberOfTrailingZeros(EFB);
    private static final int STRIDE = Long.SIZE;

    private final Format format;
    private int state; // The state of the format, kept from one page to the next

    /**
     * Return a new {@code VectorPageParser}.
     *
     * @param provider A provider of {@link Format}s.
     */
    public VectorPageParser(Format.Provider<? extends Format> provider) {
        this.format = provider.provide();
        this.state = format.base();
    }

    @Override
    public void parse(Page page) {
        final Format format = this.format;
        final char[] buf = page.buf;
        final int limit = page.limit;

        long bitmask = 0L;
        int strideStart = page.parsed - STRIDE;
        int s = state;

        while (true) {
            int pos;
            int shift;

            do {
                // Optimization: The most frequent actions are to start and to end a field. For example, in a line with
                // 5 fields there are 10 field-start/field-stop and only 1 end-of-line. We inserted a tighter loop,
                // saving a comparison per outer loop iteration.
                do {
                    // Calculate a bitmask with 1's set on the characters of interest and loop only on them. The page is
                    // padded, so a stride never reads past the end of the buffer.
                    while (bitmask == 0L) {
                        strideStart += STRIDE;
                        bitmask = format.bitmask(s, buf, strideStart);
                    }
                    shift = Long.numberOfTrailingZeros(bitmask);
                    bitmask &= bitmask - 1L;
                    pos = strideStart + shift;

                    int c;
                    if (pos < limit) {
                        c = buf[pos];
                    } else if (page.isLast()) {
                        c = -1;
                    } else {
                        state = s;
                        page.stop();
                        return;
                    }

                    s = format.consumeSlow(s, c);

                    if (isStartOfField(s)) {
                        page.startField(pos);
                    }
                    if (isEndOfField(s)) {
                        page.endField(pos - ((s & EFB) >>> EFB_TRAILING_ZEROES));
                    }
                } while (isJustStartOfFieldOrEndOfField(s));

                if (isPastDirtyOrReplay(s)) {
                    if (isPastDirty(s)) {
                        page.dirty(pos - 1);
                    }
                    if (isReplay(s)) {
                        bitmask |= 1L << shift;
                    }
                }
            } while (isNotEndOfLineAndNotEndOfFileAndNotError(s));

            if (isEndOfLine(s)) {
                page.endLine();
                page.startLine(isReplay(s) ? pos : pos + 1); // A replayed character belongs to the next line
            }
            if (isEndOfFile(s)) {
                page.finish();
                return;
            }
            if (isError(s)) {
                page.fail(new UnexpectedCharacterException(page.base + pos));
                return;
            }
        }
    }

    private boolean isEndOfField(int s) {
        return (s & (EFH | EFB)) != 0;
    }

    private boolean isEndOfLine(int s) {
        return (s & (ELH | ELB)) != 0;
    }

    private boolean isEndOfFile(int s) {
        return (s & STP) != 0;
    }

    private boolean isError(int s) {
        return (s & ERH) != 0;
    }

    private boolean isJustStartOfFieldOrEndOfField(int s) {
        return (s & (RMB | RPL | ELH | ELB | ERH | STP)) == 0;
    }

    private boolean isNotEndOfLineAndNotEndOfFileAndNotError(int s) {
        return (s & (ELH | ELB | ERH | STP)) == 0;
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
}
