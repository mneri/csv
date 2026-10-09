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
 * A {@link PageParser} that feeds every character of the page to the format.
 *
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public final class ScalarPageParser implements PageParser {
    private static final int EFB_TRAILING_ZEROES = Integer.numberOfTrailingZeros(EFB);

    private final Format format;
    private int state; // The state of the format, kept from one page to the next

    /**
     * Return a new {@code ScalarPageParser}.
     *
     * @param provider A provider of {@link Format}s.
     */
    public ScalarPageParser(Provider<? extends Format> provider) {
        this.format = provider.provide();
        this.state = format.base();
    }

    @Override
    public void parse(Page page) {
        final Format format = this.format;
        final char[] buf = page.buffer;
        final int limit = page.limit;

        int pos = page.fresh;
        int s = state;

        while (true) {
            do {
                // Optimization: The most frequent actions are to start and to end a field. For example, in a line with
                // 5 fields there are 10 field-start/field-stop and only 1 end-of-line. We inserted a tighter loop,
                // saving a comparison per outer loop iteration.
                do {
                    while (pos < limit && !isAny(s = format.consume(s, buf[pos]))) {
                        pos = pos + 1;
                    }
                    if (pos >= limit) {
                        if (!page.isLast()) {
                            state = s; // The line runs past the limit: the next call goes on from this state
                            return;
                        }
                        s = format.consume(s, -1);
                    }
                    if (isStartOfField(s)) {
                        page.startField(pos);
                    }
                    if (isEndOfField(s)) {
                        page.endField(pos - ((s & EFB) >>> EFB_TRAILING_ZEROES));
                    }
                    pos = pos + 1;
                } while (isJustStartOfFieldOrEndOfField(s));
                if (isPastDirtyOrReplay(s)) {
                    if (isPastDirty(s)) {
                        page.exclude(pos - 2); // -1: refers to previous position; -1: pos was already incremented
                    }
                    if (isReplay(s)) {
                        pos = pos - 1;
                    }
                }
            } while (isNotEndOfLineAndNotEndOfFileAndNotError(s));

            if (isEndOfLine(s)) {
                page.endLine(pos);
            }
            if (isEndOfFile(s)) {
                return;
            }
            if (isError(s)) {
                page.fail(new UnexpectedCharacterException(page.base + (isReplay(s) ? pos : pos - 1)));
                return;
            }
        }
    }

    private boolean isAny(int s) {
        return (s & ANY) != 0;
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
