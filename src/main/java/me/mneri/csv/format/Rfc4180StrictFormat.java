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

import me.mneri.csv.deserializer.Deserializer;
import me.mneri.csv.exception.UnexpectedCharacterException;
import me.mneri.csv.reader.CsvReader;
import me.mneri.csv.reader.line.RecycledLine;

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
@SuppressWarnings({"Duplicates", "unused"})
public final class Rfc4180StrictFormat implements Format {
    private static final int FLD = 0; // Field
    private static final int QOT = 8; // Quotation
    private static final int BFF = 16;  // Before field
    private static final int SQT = 24; // Start quotation
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
    // - Columns: Represent character categories (mapped via the private method columnOf()).
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
       QOT,             QOT,             QOT,             QOT,             ESC,             ERR|ERH,         0,0,             // QOT
       FLD|SFH,         BFF|SFH|EFH,     CAR|SFH|EFH,     ERR|ERH,         SQT,             EOF|SFH|EFH|ELH, 0,0,             // BFF
       QOT|SFH,         QOT,             QOT,             QOT,             SQE,             ERR|ERH,         0,0,             // SQT
       ERR|ERH,         BFF|EFB,         CAR|EFB,         ERR|ERH,         QOT|RMB,         EOF|EFB|ELH|STP, 0,0,             // ESC
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

    public static Format.Provider<Rfc4180StrictFormat> provider() {
        return Rfc4180StrictFormat::new;
    }

    private Rfc4180StrictFormat() {
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public int base() { // Bytecode size: 3 (OpenJDK 26)
        return BFL;
    }

    /**
     * {@inheritDoc}
     *
     * @param s      {@inheritDoc}
     * @param buff   {@inheritDoc}
     * @param offset {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public long bitmask(int s, byte[] buff, int offset) {
        long bm = FormatHelper.bitmask(buff, offset, (byte) -1, (byte) '\n', (byte) '\r', (byte) '"', (byte) ',');

        // A bm with 1's set at the positions of commas or any other CSV special character is not sufficient; for
        // example, the Format needs to consume a comma to track the end of the current field and the character after to
        // track the start of the next field (and the same goes for new lines and double quotes). So, after we first
        // calculated the bitmask of the CSV special characters, we add 1's for the characters positioned after them.
        bm = bm | (bm << 1);

        // We also might need to set the first bit: the Format needs to consume the character at the start of field! We
        // set it unless we're already inside a field (FLD or QOT). The states FLD and QOT are conveniently positioned
        // at the top of the DFA, so anything greater is an outside-the-field state.
        return (s & 0xFF_FF) >= (QOT + 8) ? (bm | 1L) : bm; // The QOT line is 8 integers
    }

    /**
     * {@inheritDoc}
     *
     * @param s      {@inheritDoc}
     * @param buff   {@inheritDoc}
     * @param offset {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public long bitmask(int s, char[] buff, int offset) {
        long bm = FormatHelper.bitmask(buff, offset, (char) -1, '\n', '\r', '"', ',');

        // A bm with 1's set at the positions of commas or any other CSV special character is not sufficient; for
        // example, the Format needs to consume a comma to track the end of the current field and the character after to
        // track the start of the next field (and the same goes for new lines and double quotes). So, after we first
        // calculated the bitmask of the CSV special characters, we add 1's for the characters positioned after them.
        bm = bm | (bm << 1);

        // We also might need to set the first bit: the Format needs to consume the character at the start of field! We
        // set it unless we're already inside a field (FLD or QOT). The states FLD and QOT are conveniently positioned
        // at the top of the DFA, so anything greater is an outside-the-field state.
        return (s & 0xFF_FF) >= (QOT + 8) ? (bm | 1L) : bm; // The QOT line is 8 integers
    }

    /**
     * Return the column index of the specified character in the matrix of the finite-state automaton parser.
     *
     * @param c The character.
     * @return The column index of the specified character.
     */
    private int columnOf(int c) { // Bytecode size: 34 (OpenJDK 26)
        // The implementation is equivalent to the following code:
        // if (c == ',') {
        //     return 1;
        // } else if (c == '\r') {
        //     return 2;
        // } else if (c == '\n') {
        //     return 3;
        // } else if (c == '"') {
        //     return 4;
        // } else if (c == -1) {
        //     return 5;
        // } else {
        //     return 0;
        // }

        // Fast Path: Check if 'c' is an "ordinary" character. This includes anything > 44 (standard text) or characters
        // <= 44 not in the special mask.
        // Java's shift operators natively mask the shift by 63 (c & 63). Thus, 1L << -1 cleanly wraps to bit 63.
        // Mask 0x8000_1004_0000_2400L has bits set at: 10 (\n), 13 (\r), 34 ("), 44 (,), 63 (EOF).
        if (c > ',' || ((1L << c) & 0x80_00_10_04_00_00_24_00L) == 0) {
            return 0;
        }

        // Special Path: Map the character to a 3-bit column index via bit-field extraction.
        // We use (c + 1) to shift the range of potential inputs from [-1, 44] (that is [EOF, ',']) to [0, 45].
        // Data Map 0x20_20_00_00_98_05L encodes:
        // Bits [00-02]: 5 (EOF)  | Bits [11-13]: 3 (\n) | Bits [14-16]: 2 (\r)
        // Bits [35-37]: 4 (")    | Bits [45-47]: 1 (,)
        return (int) (0x00_00_20_20_00_00_98_05L >>> (c + 1L)) & 0x7;
    }

    /**
     * {@inheritDoc}
     *
     * @param {@inheritDoc}
     * @param {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public int consume(int s, int c) { // Bytecode size: 27 (OpenJDK 26)
        // Super-Hot Path: Standard CSV data (letters, numbers, etc.) is the most common case. If the current state is
        // FLD (Inside Field) and the character is 'ordinary' (> 44), we bypass the bit-masking and array lookup
        // entirely to return the FLD state. This turns a potential memory access into a simple register comparison.
        if (s == FLD && c > ',') {
            return FLD;
        }
        return consumeSlow(s, c);
    }

    /**
     * {@inheritDoc}
     *
     * @param {@inheritDoc}
     * @param {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public int consumeSlow(int s, int c) {
        // Calculate the combined index (state (row) + character (column)). State values (BFL, BFF, etc.) are multiples
        // of 8, meaning the lower 3 bits are 0. indexOf(c) returns 0-7. Using '|' effectively performs addition without
        // carry. We mask with 0x7F (127) to stay within the 128-element DFA table; this hints to the JIT compiler to
        // eliminate array bounds checking.
        return DFA[(s | columnOf(c)) & 0x7F];
    }
}
