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

package me.mneri.csv.format;

/**
 * A specific CSV dialect.
 */
public interface Format {
    int SFH = 1 << 16; // Start field at the current position.
    int EFH = 1 << 17; // End field at the current position.
    int EFB = 1 << 18; // End field at the previous position.
    int ELH = 1 << 19; // End line at the current position.
    int RLR = 1 << 20; // Repeat last read.
    int RCB = 1 << 21; // Remove the character at the previous position.
    int STP = 1 << 22; // Stop processing the current line.
    int ERH = 1 << 23; // Error at the current position.

    int ANY = 0x0FFF << 16;

    /**
     *
     * @param <T>
     */
    interface Provider<T extends Format> {
        /**
         * Return a new {@link Format} instance.
         * <p>
         * Implementors of this interface must guarantee a new instance is returned for each call.
         *
         * @return A fresh format instance.
         */
        T provide();
    }


    /**
     * Return the initial state.
     *
     * @return An integer encoding the initial state.
     */
    int base();

    /**
     * Given the current state and a character, return an integer encoding both the next state and the action to
     * perform.
     *
     * @param s The current state as returned by a previous call to {@link Format#base()} or this method.
     * @param c The character.
     * @return An integer encoding both the next state and the action to perform.
     */
    int consume(int s, int c);
}
