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

package me.mneri.csv.reader

import java.util.concurrent.ConcurrentHashMap

/**
 * A stream for the tests of the readers: it records the threads that read it and whether it has been closed, and it can
 * fail after a number of characters.
 */
class RecordingReader extends Reader {
    final Set<Thread> threads = ConcurrentHashMap.newKeySet()
    volatile boolean closed
    private final Reader input
    private final int failAfter
    private final Throwable failure
    private int position

    /**
     * Return a new stream of the text. The options are {@code failAfter}, the number of characters after which the
     * stream throws, and {@code failure}, what it throws: an exception or an error.
     */
    RecordingReader(Map options = [:], String text) {
        this.input = new StringReader(text)
        this.failAfter = options.failAfter ?: Integer.MAX_VALUE
        this.failure = options.failure
    }

    @Override
    int read(char[] buf, int off, int len) throws IOException {
        threads << Thread.currentThread()
        if (position == failAfter) {
            throw failure
        }
        int read = input.read(buf, off, Math.min(len, failAfter - position))
        if (read > 0) {
            position += read
        }
        return read
    }

    @Override
    void close() throws IOException {
        closed = true
        input.close()
    }
}
