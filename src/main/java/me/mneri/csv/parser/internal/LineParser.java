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

import me.mneri.csv.line.internal.InternalRecycledLine;

import java.io.Closeable;
import java.io.IOException;

/**
 * Parse the internal character stream and return CSV lines.
 *
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public interface LineParser extends Closeable {
    /**
     * Closes the stream and releases any system resources associated with it. Once the stream has been closed, further
     * {@link #next(InternalRecycledLine)} invocations will throw an {@link IOException}. Closing a previously closed
     * stream has no effect.
     *
     * @throws IOException If an I/O error occurs.
     */
    @Override
    void close() throws IOException;

    /**
     * Advance the internal character stream and parse the next CSV line.
     *
     * @param out The output parameter where the next CSV line is saved.
     * @return {@code true} if a line has been returned, {@code false} otherwise.
     * @throws IOException If an I/O error occurs.
     */
    boolean next(InternalRecycledLine out) throws IOException;
}
