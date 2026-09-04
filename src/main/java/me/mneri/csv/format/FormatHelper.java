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

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorSpecies;

public final class FormatHelper {
    private static final VectorSpecies<Byte> BYTE_SPECIES = ByteVector.SPECIES_PREFERRED;
    private static final int BYTE_STRIDE = BYTE_SPECIES.length();

    private static final VectorSpecies<Short> SHORT_SPECIES = ShortVector.SPECIES_PREFERRED;
    private static final int SHORT_STRIDE = SHORT_SPECIES.length();

    private FormatHelper() {
    }

    static long bitmask(byte[] cb, int offset, byte c1, byte c2, byte c3, byte c4) {
        long bitmask = 0L;
        for (int i = 0; i < Long.SIZE; i += BYTE_STRIDE) {
            ByteVector vector = ByteVector.fromArray(BYTE_SPECIES, cb, offset + i);
            bitmask |= vector
                    .eq(c1)
                    .or(vector.eq(c2))
                    .or(vector.eq(c3))
                    .or(vector.eq(c4)).
                    toLong() << i;
        }
        return bitmask;
    }

    static long bitmask(byte[] cb, int offset, byte c1, byte c2, byte c3, byte c4, byte c5) {
        long bitmask = 0L;
        for (int i = 0; i < Long.SIZE; i += BYTE_STRIDE) {
            ByteVector vector = ByteVector.fromArray(BYTE_SPECIES, cb, offset + i);
            bitmask |= vector
                    .eq(c1)
                    .or(vector.eq(c2))
                    .or(vector.eq(c3))
                    .or(vector.eq(c4))
                    .or(vector.eq(c5))
                    .toLong() << i;
        }
        return bitmask;
    }

    static long bitmask(char[] cb, int offset, char c1, char c2, char c3, char c4) {
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

    static long bitmask(char[] cb, int offset, char c1, char c2, char c3, char c4, char c5) {
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
