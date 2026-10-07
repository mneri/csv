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

import me.mneri.csv.extension.internal.Extensions;
import me.mneri.csv.format.Format;
import me.mneri.csv.hint.Hint;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.nio.BufferOverflowException;

/**
 * Load pages: read characters from the stream, then parse them into lines.
 *
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public final class PageLoader implements Closeable {
    private final Reader in;
    private final PageParser parser;

    /**
     * Return a new {@code PageLoader}.
     *
     * @param in       The reader.
     * @param provider A provider of {@link Format}s.
     * @param hints    The hints to the reader.
     */
    public PageLoader(Reader in, Format.Provider<? extends Format> provider, long hints) {
        this.in = in;
        if (Extensions.SIMD_SUPPORTED && (hints & Hint.TINY_FIELDS) == 0) {
            this.parser = new VectorPageParser(provider);
        } else {
            this.parser = new ScalarPageParser(provider);
        }
    }

    /**
     * Fill the page, which already holds the tail of the previous page, and parse it. Errors are recorded in the page
     * and thrown to the client after the page's lines.
     *
     * @param page The page.
     */
    public void load(Page page) {
        try {
            page.fill(in);
            parser.parse(page);
            if (page.tail == 0 && !page.isLast()) { // A single line fills the whole page
                page.fail(new BufferOverflowException());
            }
        } catch (IOException | RuntimeException e) {
            page.fail(e);
        }
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
