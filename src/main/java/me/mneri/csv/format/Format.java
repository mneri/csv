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
 * A specific CSV dialect. Instances of this interface manage the state of the CSV parser and dictate the actions a
 * client must perform for each character in a stream.
 * <p>
 * For performance reasons, the state is maintained externally rather than encapsulated within the {@code Format}
 * instance, and it is encoded in a primitive {@code int}. The client must keep track of this value and pass it back
 * when calling methods of the {@code Format}; for example, the methods {@link #consume(int, int)} and
 * {@link #consumeSlow(int, int)} take the current state and the next character, and return the new state back to the
 * client.
 * <p>
 * If the implementations of {@code Format} keep their methods relatively short, the HotSpot C2 compiler could map the
 * state directly to a CPU register which is orders of magnitude faster than accessing a heap-allocated object field.
 * <p>
 * Within the state, {@code Format} encodes actions in the form of flags. The client must test for these flags and
 * perform the corresponding action. Some flags tell the client to perform an action at the current character's
 * position, while others at the position of the <i>previous</i> character in the stream.
 * <p>
 * <b>Example usage:</b>
 * <pre>{@code
 * int state = format.base();
 * while (true) {
 *     int c = getNextChar();
 *     state = format.consume(state, c);
 *
 *      // If the "start field" flag is set
 *     if ((state & SFH) != 0) {
 *         // ...
 *     }
 *     // ...
 * }
 * }</pre>
 */
public interface Format {
    /**
     * When this flag is set, the client must take action to start a new field at the <i>current</i> character's
     * position.
     */
    int SFH = 1 << 16;

    /**
     * When this flag is set, the client must take action to end the current field at the <i>current</i> character's
     * position.
     */
    int EFH = 1 << 17;

    /**
     * When this flag is set, the client must take action to end the current field at the position of the
     * <i>previous</i> character in the stream.
     */
    int EFB = 1 << 18;

    /**
     * When this flag is set, the client must take action to end the line at the <i>current</i> character's position.
     */
    int ELH = 1 << 19;

    /**
     * When this flag is set, the client must take action to go back one character in the stream.
     */
    int RPL = 1 << 20;

    /**
     * When this flag is set, the client must take action to remove the character at <i>previous</i> position.
     */
    int RMB = 1 << 21;

    /**
     * When this flag is set, the client is advised that the end-of-file has been reached successfully.
     */
    int STP = 1 << 22;

    /**
     * When this flag is set, the client must take action to report the CSV was invalid and contains format errors.
     */
    int ERH = 1 << 23;

    /**
     * Convenience mask to check if any action flag is set. Since the vast majority of characters in a stream don't
     * carry an action, using this mask allows the client to quickly skip redundant checks.
     */
    int ANY = 0x0FFF << 16;

    /**
     * Provide instances of {@code Format}.
     *
     * @param <T> The {@code Format} class.
     */
    interface Provider<T extends Format> {
        /**
         * Return a new {@link Format} instance.
         * <p>
         * Implementors of this interface must guarantee a new instance is returned for each call.
         *
         * @return A fresh {@link Format} instance.
         */
        T provide();
    }

    interface Simd {
        /**
         * Given a {@link ShortVector} filled with CSV data, return a bitmask with bits set in state-changing positions.
         * <p>
         * The bitmask indicates which characters in the array must be processed by the {@code Format}, and which can be
         * ignored. In the example below, bits are set at key positions (such as the commas). Passing only these characters
         * to the {@link #consume(int, int)} or {@link #consumeSlow(int, int)} methods is all that is needed for the
         * {@code Format}'s state machine to remain consistent, while the intermediate characters (i.e. the characters with
         * bits set to zero) can be safely skipped.
         * <pre>
         * CSV chunk: a a a a , b b b b , c c c c \r\n
         * Bitmask:   1 0 0 0 1 1 0 0 0 1 1 0 0 0 1 1
         * </pre>
         * The implementation uses SIMD (Single Instruction, Multiple Data) instructions to evaluate the entire vector
         * concurrently, returning the bitmask in just a few CPU cycles.
         * <p>
         * <i>Please, note that the method can sometimes return false-positives, but never false-negatives.</i>
         *
         * @param s       The current state as returned by a previous call to {@link #base()}, {@link #consume(int, int)} or
         *                {@link #consumeSlow(int, int)}.
         * @param species The species of the CPU's SIMD registers.
         * @param source  The source character array.
         * @param offset  The offset in the source array.
         * @return A bitmask.
         */
        long bitmask(int s, VectorSpecies<Short> species, char[] source, int offset);
    }

    /**
     * Return the initial state.
     *
     * @return An integer encoding the initial state.
     */
    int base();

    /**
     * Given the current state and a character, return an integer encoding both the next state and the actions to
     * perform.
     *
     * @param s The current state as returned by a previous call to {@link #base()}, {@link #consumeSlow(int, int)} or
     *          this method.
     * @param c The character.
     * @return An integer encoding both the next state and the actions to perform.
     */
    int consume(int s, int c);

    /**
     * Given the current state and a character, return an integer encoding both the next state and the actions to
     * perform.
     * <p>
     * This is a deoptimized version of {@link #consume(int, int)} and as such it is guaranteed to return the same
     * values. {@link #consume(int, int)} might contain shortcuts on the assumption that most of the characters are not
     * CSV control characters. This method makes the opposite assumption.
     *
     * @param s The current state as returned by a previous call to {@link #base()}, {@link #consume(int, int)} or this
     *          method.
     * @param c The character.
     * @return An integer encoding both the next state and the actions to perform.
     */
    int consumeSlow(int s, int c);

    /**
     * Return the {@code Format}'s SIMD extension.
     * <p>
     * <b>Warning:</b> calling this method when the Vector API is not enabled will result in a runtime exception.
     *
     * @return The SIMD extension.
     */
    Simd simd();
}
