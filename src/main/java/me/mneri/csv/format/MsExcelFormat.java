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

import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Implements a <i>fully relaxed</i> interpretation of the RFC4180 standard mirroring the behaviour of Microsoft Excel,
 * which accepts
 * <p>
 * The following features are supported:
 * <ul>
 *     <li>
 *         <strong>Variable number of fields</strong>: lines may contain a different number of fields from one another.
 *         For example:<br/>
 *         <samp>
 *             aaa,bbb,ccc CRLF<br/>
 *             xxx,yyy CRLF
 *         </samp>
 *     </li>
 *     <li>
 *         <strong>Line termination</strong>: lines can end with {@code \r\n}, {@code \r}, or {@code \n}; files can be
 *         inconsistent in their line termination, using different line terminators on different lines, any number of
 *         times. For example:<br/>
 *         <samp>
 *             aaa,bbb,ccc CRLF<br/>
 *             xxx,yyy,zzz LF
 *         </samp>
 *     </li>
 *     <li>
 *         <strong>Fields containing double quotes</strong>: fields that do not begin with a double quotes character
 *         ({@code "}) may contain double quotes; in such cases, double quotes are treated as ordinary characters. For
 *         example:<br/>
 *         <samp>
 *             aaa,b"b"b,ccc CRLF ; interpreted as &lt;aaa&gt;, &lt;b"b"b&gt; and &lt;ccc&gt;<br/>
 *             xxx,y"y,zzz CRLF   ; interpreted as &lt;xxx&gt;, &lt;y"y&gt; and &lt;zzz&gt;
 *         </samp>
 *     </li>
 *     <li>
 *         <strong>Extra text after a double quoted field</strong>: fields that begin with a double quotes character
 *         ({@code "}) may include additional text after the closing double quotes and before the comma delimiter
 *         ({@code ,}); this additional text is treated as part of the field, following the rules for unquoted fields.
 *         For example:<br/>
 *         <samp>
 *             aaa,"bb"b,ccc CRLF ; interpreted as &lt;aaa&gt;, &lt;bbb&gt; and &lt;ccc&gt;<br/>
 *             xxx,"y"yy",zzz CRLF ; interpreted as &lt;xxx&gt;, &lt;yyy"&gt; and &lt;zzz&gt;
 *         </samp>
 *     </li>
 *     <li>
 *         <strong>Termination of double quoted fields</strong>: if a field starts with a double quote character
 *         ({@code "}) and the end of file is reached prior to the corresponding closing double quote, the field shall
 *         still be regarded as correctly terminated. For example:<br/>
 *         <samp>
 *             aaa,bbb,"ccc EOF ; interpreted as &lt;aaa&gt;, &lt;bbb&gt; and &lt;ccc&gt;<br/>
 *         </samp>
 *     </li>
 *     <li>
 *         <strong>Locale-dependent delimiter</strong>: the field delimiter is {@code ,} in some locales, while is
 *         {@code ;} in others. For example:<br/>
 *         <samp>
 *             aaa;bbb;"ccc EOF ; interpreted as &lt;aaa&gt;, &lt;bbb&gt; and &lt;ccc&gt; if the locale is IT-it<br/>
 *         </samp>
 *     </li>
 * </ul>
 *
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
@SuppressWarnings({"Duplicates", "unused"})
public final class MsExcelFormat implements Format {
    private static final int FLD = 0; // Field
    private static final int QOT = 8; // Quotation
    private static final int BFF = 16; // Before field
    private static final int SQT = 24; // Start quotation
    private static final int ESC = 32; // Escape
    private static final int SQE = 40; // Escape at start quotation
    private static final int BFL = 48; // Before line
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
    // *                  ,                    \r                   \n                   "                    EOF                  padding
       FLD,               BFF|EFH,             CAR|EFH,             BFL|EFH|ELH,         FLD,                 EOF|EFH|ELH|STP,     0,0,  // FLD
       QOT,               QOT,                 QOT,                 QOT,                 ESC,                 EOF|EFH|ELH|STP,     0,0,  // QOT
       FLD|SFH,           BFF|SFH|EFH,         CAR|SFH|EFH,         BFL|SFH|EFH|ELH,     SQT,                 EOF|SFH|EFH|ELH|STP, 0,0,  // BFF
       QOT|SFH,           QOT|SFH,             QOT|SFH,             QOT|SFH,             SQE,                 EOF|SFH|EFH|ELH|STP, 0,0,  // SQT
       FLD|RMB,           BFF|EFB,             CAR|EFB,             BFL|EFB|ELH,         QOT|RMB,             EOF|EFB|ELH|STP,     0,0,  // ESC
       FLD|SFH,           BFF|SFH|EFH,         CAR|SFH|EFH,         BFL|SFH|EFH|ELH,     QOT|SFH,             EOF|SFH|EFH|ELH|STP, 0,0,  // SQE
       FLD|SFH,           BFF|SFH|EFH,         CAR|SFH|EFH,         BFL|SFH|EFH|ELH,     SQT,                 EOF|STP,             0,0,  // BFL *
       BFL|ELB|RPL,       BFL|ELB|RPL,         BFL|ELB|RPL,         BFL|ELH,             BFL|ELB|RPL,         EOF|ELB|STP,         0,0,  // CAR
       ERR|ERH,           ERR|ERH,             ERR|ERH,             ERR|ERH,             ERR|ERH,             ERR|ERH,             0,0,  // EOF
       ERR|ERH,           ERR|ERH,             ERR|ERH,             ERR|ERH,             ERR|ERH,             ERR|ERH,             0,0,  // ERR
       0,                 0,                   0,                   0,                   0,                   0,                   0,0,
       0,                 0,                   0,                   0,                   0,                   0,                   0,0,
       0,                 0,                   0,                   0,                   0,                   0,                   0,0,
       0,                 0,                   0,                   0,                   0,                   0,                   0,0,
       0,                 0,                   0,                   0,                   0,                   0,                   0,0,
       0,                 0,                   0,                   0,                   0,                   0,                   0,0};
    //@formatter:on
    //@formatter:on

    /**
     * Return a provider of {@code MsExcelFormat} instances for the specified locale.
     *
     * @param locale The locale.
     * @return The provider.
     */
    public static Provider<MsExcelFormat> provider(Locale locale) {
        return () -> new MsExcelFormat(locale);
    }

    private int del;
    private int high;
    private long map;
    private long mask;

    private MsExcelFormat(Locale locale) {
        int ds = DecimalFormatSymbols.getInstance(locale).getDecimalSeparator();
        int del = (ds != ',') ? ',' : ';';
        init(del);
    }

    private void init(int del) {
        this.del = del;
        this.high = Math.min(del, Long.SIZE - 1);
        // Java's shift operators natively mask the shift by 63 (c & 63). Thus, 1L << -1 cleanly wraps to bit 63.
        // Mask 0x80_00_00_04_00_00_24_00L has bits set at: 10 (\n), 13 (\r), 34 ("), 63 (EOF), then we set the bit
        // for the delimiter (e.g. 44 (,), or 59 (;)).
        this.mask = 0x80_00_00_04_00_00_24_00L | (1L << del);
        // Data Map 0x00_00_00_20_00_00_98_05L encodes:
        // Bits [00-02]: 5 (EOF)  | Bits [11-13]: 3 (\n) | Bits [14-16]: 2 (\r)
        // Bits [35-37]: 4 (")
        // Then, we add the bits for the delimiter.
        this.map = 0x00_00_00_20_00_00_98_05L | (0x01L << (del + 1));
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
    public long bitmask(int s, char[] buff, int offset) {
        long bm = FormatHelper.bitmask(buff, offset, (char) -1, '\n', '\r', '"', (char) del);

        // A bitmask with 1's set at the positions of commas or any other CSV special character is not sufficient; for
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
     * Return the column index of the specified character in the matrix of the finite state automaton parser.
     *
     * @param c The character.
     * @return The column index of the specified character.
     */
    private int columnOf(int c) { // Bytecode size: 34 (OpenJDK 26)
        // The implementation is equivalent to the following code:
        // if (c == sep) {
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

        // The implementation below uses a mask and a map. While likely slightly slower than a pure, properly ordered
        // if-else chain like the one above, it is compact and more likely to be inlined, which has a greater benefit on
        // the overall performance.

        // Fast Path: Check if 'c' is an "ordinary" character. This includes anything > sep (standard text) or
        // characters <= sep not in the special mask.
        if (c > high || ((1L << c) & mask) == 0) {
            return 0;
        }

        // Special Path: Map the character to a 3-bit column index via bit-field extraction.
        // We use (c + 1) to shift the range of potential inputs from [-1, sep] (that is [EOF, sep]) to [0, sep + 1].
        return (int) (map >>> (c + 1L)) & 0x7;
    }

    /**
     * {@inheritDoc}
     *
     * @param s {@inheritDoc}
     * @param c {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public int consume(int s, int c) { // Bytecode size: 27 (OpenJDK 26)
        // Super-Hot Path: Standard CSV data (letters, numbers, etc.) is the most common case. If the current state is
        // FLD (Inside Field) and the character is 'ordinary' (> sep), we bypass the bit-masking and array lookup
        // entirely to return the FLD state. This turns a potential memory access into a simple register comparison.
        if (s == FLD && c > del) {
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

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public int delimiter() {
        return del;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public int qualifier() {
        return '"';
    }
}
