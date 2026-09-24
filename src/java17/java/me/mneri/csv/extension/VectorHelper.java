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

package me.mneri.csv.extension;

import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * Helper class for SIMD operations.
 * <p>
 * This class uses the Vector API, which is an incubator feature not yet available in standard Java installations, but
 * can be enabled by adding the JVM flag {@code --add-modules jdk.incubator.vector}.
 *
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public final class VectorHelper {
    private static final VectorSpecies<Short> SHORT_SPECIES = ShortVector.SPECIES_PREFERRED;
    private static final int SHORT_STRIDE = SHORT_SPECIES.length();

    private VectorHelper() {
    }

    /**
     * Given an array, read a chunk of 64 characters and return a bitmask with 1's set in the positions of the specified
     * characters.
     * <p>
     * Clients must guarantee that {@code cb.length - offset} is at least 64; in other words, clients must guarantee
     * that there are at least 64 characters available to read.
     * <p>
     * The implementation uses SIMD (Single Instruction, Multiple Data) instructions to evaluate the entire vector
     * concurrently, returning the bitmask in just a few CPU cycles.
     *
     * @param cb     The array.
     * @param offset The starting offset.
     * @param ule    A character checked for unsigned less or equal.
     * @param eq1    A character checked for equality.
     * @param eq2    A character checked for equality.
     * @return A bitmask with 1's set in the positions of the specified characters.
     */
    static public long bitmaskUleEqEq(char[] cb, int offset, char ule, char eq1, char eq2) {
        long bitmask = 0L;
        for (int i = 0; i < Long.SIZE; i += SHORT_STRIDE) {
            ShortVector vector = ShortVector.fromCharArray(SHORT_SPECIES, cb, offset + i);
            bitmask |= vector
                    .compare(VectorOperators.UNSIGNED_LE, (short) ule)
                    .or(vector.eq((short) eq1))
                    .or(vector.eq((short) eq2))
                    .toLong() << i; // There's a shift here!
        }
        return bitmask;
    }
}
