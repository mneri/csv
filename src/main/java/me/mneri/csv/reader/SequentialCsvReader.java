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

import me.mneri.csv.deserializer.Deserializer;
import me.mneri.csv.parser.internal.InternalRecycledLine;
import me.mneri.csv.parser.internal.Page;
import me.mneri.csv.parser.internal.PageLoader;

import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * A {@link CsvReader} that loads pages on the calling thread, reusing a single page.
 * <p>
 * The page is as large as the longest line accepted. When it's small, the lines parsed from it are still in the L1
 * cache when they are deserialized: in our measurements, an 8K page is 20% faster than a 64K page.
 *
 * @param <T> The type of the Java objects to read.
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
final class SequentialCsvReader<T> extends CsvReader<T> {
    private static final int STATE_OPEN = 0;
    private static final int STATE_CLOSED = 1;

    private final PageLoader loader;
    private final Deserializer<T> deserializer;
    private final Page page;

    private int cursor;
    private int state = STATE_OPEN;

    SequentialCsvReader(int pageSize, PageLoader loader, Deserializer<T> deserializer) {
        this.loader = loader;
        this.deserializer = deserializer;
        this.page = new Page(pageSize);
    }

    @Override
    public void close() throws IOException {
        state = STATE_CLOSED;
        cursor = Integer.MAX_VALUE;
        loader.close();
    }

    @Override
    public boolean hasNext() throws IOException {
        return cursor < page.lineCount() || hasNext2();
    }

    private boolean hasNext2() throws IOException {
        if (state != STATE_OPEN) {
            throw new IllegalStateException("The reader is closed.");
        }
        do {
            if (page.hasError()) {
                page.rethrow();
            }
            if (page.isLast()) {
                return false;
            }
            flip();
        } while (page.lineCount() == 0);

        cursor = 0;
        return true;
    }

    @Override
    public T next() throws IOException {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        InternalRecycledLine line = page.line(cursor++);
        return deserializer.deserialize(line);
    }

    private void flip() {
        page.carryOverTo(page);
        loader.load(page);
    }
}
