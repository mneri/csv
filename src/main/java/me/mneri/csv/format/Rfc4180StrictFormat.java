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

import me.mneri.csv.exception.UnexpectedCharacterException;
import me.mneri.csv.reader.CsvReader;
import me.mneri.csv.reader.Deserializer;
import me.mneri.csv.reader.RecycledLine;

/**
 * Implements a strict interpretation of the RFC4180 standard for CSV files, with one simple variation:
 * <ul>
 *     <li>
 *         <b>Variable number of fields</b>: lines may contain a different number of fields from one another. For
 *         example:<br/>
 *         <samp>
 *             aaa,bbb,ccc CRLF<br/>
 *             xxx,yyy CRLF
 *         </samp>
 *     </li>
 * </ul>
 * If the number of fields is a strict requirement, clients shall perform the validation in
 * {@link Deserializer#deserialize(RecycledLine)}, on the deserializer instance given to the {@link CsvReader}.
 * <p>
 * This strict interpretation forces the {@link CsvReader} to throw a {@link UnexpectedCharacterException} whenever one
 * of the rules specified in RFC4180 is broken (with the exclusion of the variable number of fields, as explained
 * above).
 * <p>
 * There are two progressively relaxed variations of the {@code Rfc4180StrictFormat}:
 * {@link Rfc4180HalfRelaxedFormat} which relaxes some of the rules but still throws
 * {@link UnexpectedCharacterException} under a number of circumstances, and {@link Rfc4180FullyRelaxedFormat} which
 * always guarantees to have a <i>best-effort</i> interpretation of a non-compliant CSV file and never throw an
 * exception.
 */
@SuppressWarnings({"Duplicates", "Unused"})
public final class Rfc4180StrictFormat implements Format {
    private static final int FLD = 0; // Field
    private static final int BFF = 8;  // Before field
    private static final int SQT = 16; // Start quotation
    private static final int QOT = 24; // Quotation
    private static final int ESC = 32; // Escape
    private static final int SQE = 40; // Escape at start quotation
    private static final int BFL = 48;  // Before line
    private static final int CAR = 56; // Carriage return
    private static final int EOF = 64; // End of file
    private static final int ERR = 72; // Error

    // The parser uses a deterministic finite state automaton (DFA) represented as a flattened 2D matrix.
    //
    // Structure:
    // - Rows: Represent current states (BFL, BFF, etc.).
    // - Columns: Represent character categories (mapped via the private method indexOf()).
    // - Cells: Each 32-bit integer packs the next state in the lower 16 bits and transition actions in the upper 16
    //   bits.
    //
    // Performance Optimizations:
    // 1. Padding: Each row is padded to 8 elements (a power of 2) to allow index calculation via bitwise OR/SHIFT
    //    rather than multiplication.
    // 2. Alignment: The matrix is sized to 128 elements to hint at array-bounds elimination during JIT compilation.
    // 3. Bit-Packing: State and actions are retrieved in a single memory access.
    //
    // Example: In state FLD ('inside field'), a comma input yields state BFF ('before field') and triggers the EFH
    // ('end field here') action.

    //@formatter:off
    private static final int[] DFA = {
    // *                ,                \r               \n               "                EOF              padding
       FLD,             BFF|EFH,         CAR|EFH,         ERR|ERH,         ERR|ERH,         EOF|EFH|STP,     0,0,             // FLD
       FLD|SFH,         BFF|SFH|EFH,     CAR|SFH|EFH,     ERR|ERH,         SQT,             EOF|SFH|EFH|ELH, 0,0,             // BFF
       QOT|SFH,         QOT,             QOT,             QOT,             SQE,             ERR|ERH,         0,0,             // SQT
       QOT,             QOT,             QOT,             QOT,             ESC,             ERR|ERH,         0,0,             // QOT
       ERR|ERH,         BFF|EFB,         CAR|EFB,         ERR|ERH,         QOT|RCB,         EOF|EFB|ELH|STP, 0,0,             // ESC
       ERR|ERH,         BFF|SFH|EFH,     CAR|SFH|EFH,     ERR|ERH,         QOT|SFH,         EOF|SFH|EFH|STP, 0,0,             // SQE
       FLD|SFH,         BFF|SFH|EFH,     CAR|SFH|EFH,     ERR|ERH,         SQT,             EOF|STP,         0,0,             // BFL
       ERR|ERH,         ERR|ERH,         ERR|ERH,         BFL|ELH,         ERR|ERH,         ERR|ERH,         0,0,             // CAR
       ERR|ERH,         ERR|ERH,         ERR|ERH,         ERR|ERH,         ERR|ERH,         ERR|ERH,         0,0,             // EOF
       ERR|ERH,         ERR|ERH,         ERR|ERH,         ERR|ERH,         ERR|ERH,         ERR|ERH,         0,0,             // ERR
       0,               0,               0,               0,               0,               0,               0,0,
       0,               0,               0,               0,               0,               0,               0,0,
       0,               0,               0,               0,               0,               0,               0,0,
       0,               0,               0,               0,               0,               0,               0,0,
       0,               0,               0,               0,               0,               0,               0,0,
       0,               0,               0,               0,               0,               0,               0,0};
    //@formatter:on

    /**
     * Provider of {@link Rfc4180StrictFormat}.
     */
    public static final class Provider implements Format.Provider<Rfc4180StrictFormat> {
        private Provider() {
        }

        /**
         * Return a new {@link Rfc4180StrictFormat} instance.
         *
         * @return A new {@link Rfc4180StrictFormat} instance.
         */
        @Override
        public Rfc4180StrictFormat provide() {
            return new Rfc4180StrictFormat();
        }
    }

    public static Provider provider() {
        return new Provider();
    }

    /**
     * Create a new {@code Rfc4180StrictFormat} instance.
     * <p>
     * This method is private, use {@link Rfc4180StrictFormat.Provider#provide()} instead.
     */
    private Rfc4180StrictFormat() {
    }

    /**
     * {@inheritDoc}
     *
     * @return The initial state.
     */
    @Override
    public int base() {
        return BFL;
    }

    /**
     * Return the column index of the specified character in the matrix of the finite-state automaton parser.
     *
     * @param c The character.
     * @return The column index of the specified character.
     */
    private int columnOf(int c) {
        // Fast Path: Check if 'c' is an "ordinary" character. This includes anything > 44 (standard text) or characters
        // <= 44 not in the special mask.
        // We use (c + 1) to shift the range of potential inputs from [-1, 44] (that is [EOF, ',']) to [0, 45].
        // Mask 0x20_08_00_00_48_01L has bits at: 0 (EOF), 11 (\n), 14 (\r), 35 ("), 45 (,).
        if (c > ',' || ((1L << (c + 1)) & 0x20_08_00_00_48_01L) == 0) {
            return 0;
        }

        // Special Path: Map the character to a 3-bit column index via bit-field extraction.
        // Data Map 0xE0_30_00_00_C0_05L encodes:
        // Bits [00-02]: 5 (EOF)  | Bits [11-13]: 3 (\n) | Bits [14-16]: 2 (\r)
        // Bits [35-37]: 4 (")    | Bits [45-47]: 1 (,)
        return (int) (0x20_20_00_00_98_05L >> (c + 1)) & 0x7;
    }

    /**
     * {@inheritDoc}
     *
     * @param s The current state as returned by a previous call to {@link Format#base()} or this method.
     * @param c The character, or {@code -1} for {@code EOF}.
     * @return An integer encoding both the next state and the action to perform.
     */
    @Override
    public int consume(int s, int c) {
        // Super-Hot Path: Standard CSV data (letters, numbers, etc.) is the most common case. If the current state is
        // FLD (Inside Field) and the character is 'ordinary' (> 44), we bypass the bit-masking and array lookup
        // entirely to return the FLD state. This turns a potential memory access into a simple register comparison.
        if (s == FLD && c > ',') {
            return FLD;
        }

        // Calculate the combined index (state (row) + character (column)). State values (BFL, BFF, etc.) are multiples
        // of 8, meaning the lower 3 bits are 0. indexOf(c) returns 0-7. Using '|' effectively performs addition without
        // carry. We mask with 0x7F (127) to stay within the 128-element DFA table; this hints to the JIT compiler to
        // eliminate array bounds checking.
        final int i = (s | columnOf(c)) & 0x7F;
        return DFA[i];
    }
}
