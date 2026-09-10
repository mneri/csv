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

package me.mneri.csv.exception;

import java.io.IOException;

/**
 * Thrown when an invalid character is found during parsing.
 *
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public class UnexpectedCharacterException extends IOException {
    private final long position;

    /**
     * Return a new {@code UnexpectedCharacterException}.
     *
     * @param position The absolute position of the character in the stream.
     */
    public UnexpectedCharacterException(long position) {
        super("Unexpected character at position " + position);
        this.position = position;
    }

    /**
     * Return the position in the stream where the error occurred.
     *
     * @return The position.
     */
    public long getPosition() {
        return position;
    }
}
