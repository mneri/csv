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

import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorSpecies;

/**
 * Helper class for {@link Format} implementations.
 *
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public final class FormatHelper {
    private static final VectorSpecies<Short> SHORT_SPECIES = ShortVector.SPECIES_PREFERRED;
    private static final int SHORT_STRIDE = SHORT_SPECIES.length();

    private FormatHelper() {
    }

    /**
     * Given an array, read a chunk of 64 characters and return a bitmask with 1's set in the positions of the specified
     * characters.
     * <p>
     * Clients must guarantee that {@code cb.length - offset} is at least 64; in other words, clients must guarantee#
     * that there are at least 64 characters to read.
     * <p>
     * The implementation uses SIMD (Single Instruction, Multiple Data) instructions to evaluate the entire vector
     * concurrently, returning the bitmask in just a few CPU cycles.
     *
     * @param cb     The array.
     * @param offset The starting offset.
     * @param c1     A character.
     * @param c2     A character.
     * @param c3     A character.
     * @param c4     A character.
     * @return A bitmask with 1's set in the positions of the specified characters.
     */
    static public long bitmask(char[] cb, int offset, char c1, char c2, char c3, char c4) {
        long bitmask = 0L;
        for (int i = 0; i < Long.SIZE; i += SHORT_STRIDE) {
            ShortVector vector = ShortVector.fromCharArray(SHORT_SPECIES, cb, offset + i);
            bitmask |= vector
                    .eq((short) c1)
                    .or(vector.eq((short) c2))
                    .or(vector.eq((short) c3))
                    .or(vector.eq((short) c4))
                    .toLong() << i;
        }
        return bitmask;
    }

    /**
     * Given an array, read a chunk of 64 characters and return a bitmask with 1's set in the positions of the specified
     * characters.
     * <p>
     * Clients must guarantee that {@code cb.length - offset} is at least 64; in other words, clients must guarantee#
     * that there are at least 64 characters to read.
     * <p>
     * The implementation uses SIMD (Single Instruction, Multiple Data) instructions to evaluate the entire vector
     * concurrently, returning the bitmask in just a few CPU cycles.
     *
     * @param cb     The array.
     * @param offset The starting offset.
     * @param c1     A character.
     * @param c2     A character.
     * @param c3     A character.
     * @param c4     A character.
     * @param c5     A character.
     * @return A bitmask with 1's set in the positions of the specified characters.
     */
    static public long bitmask(char[] cb, int offset, char c1, char c2, char c3, char c4, char c5) {
        long bitmask = 0L;
        for (int i = 0; i < Long.SIZE; i += SHORT_STRIDE) {
            ShortVector vector = ShortVector.fromCharArray(SHORT_SPECIES, cb, offset + i);
            bitmask |= vector
                    .eq((short) c1)
                    .or(vector.eq((short) c2))
                    .or(vector.eq((short) c3))
                    .or(vector.eq((short) c4))
                    .or(vector.eq((short) c5))
                    .toLong() << i;
        }
        return bitmask;
    }
}
