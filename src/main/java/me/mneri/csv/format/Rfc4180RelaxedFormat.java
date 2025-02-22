package me.mneri.csv.format;

/**
 * Implements a <i>fully relaxed</i> interpretation of the RFC4180 standard for CSV files that can parse a higher
 * number of non-compliant CSV files than {@link Rfc4180HalfRelaxedFormat}. This version is modeled around the
 * behaviour of the Microsoft Excel CSV parser and is guaranteed to never emit errors and always offer a
 * <i>best-effort</i> interpretation of a non-compliant CSV file.
 * <p>
 * The following features are supported:
 * <ul>
 *     <li>
 *         <b>Variable number of fields</b>: lines may contain a different number of fields from one another. For
 *         example:<br/>
 *         <samp>
 *             aaa,bbb,ccc CRLF<br/>
 *             xxx,yyy CRLF
 *         </samp>
 *     </li>
 *     <li>
 *         <b>Line termination</b>: lines can end with {@code \r\n}, {@code \r}, or {@code \n}; files can be
 *         inconsistent in their line termination, using different line terminators on different lines, any number of
 *         times. For example:<br/>
 *         <samp>
 *             aaa,bbb,ccc CRLF<br/>
 *             xxx,yyy,zzz LF
 *         </samp>
 *     </li>
 *     <li>
 *         <b>Fields containing double quotes</b>: fields that do not begin with a double quotes character ({@code "})
 *         may contain double quotes; in such cases, double quotes are treated as ordinary characters. For example:<br/>
 *         <samp>
 *             aaa,b"b"b,ccc CRLF ; interpreted as &lt;aaa&gt;, &lt;b"b"b&gt; and &lt;ccc&gt;<br/>
 *             xxx,y"y,zzz CRLF   ; interpreted as &lt;xxx&gt;, &lt;y"y&gt; and &lt;zzz&gt;
 *         </samp>
 *     </li>
 *     <li>
 *         <b>Extra text after a double quoted field</b>: fields that begin with a double quotes character ({@code "})
 *         may include additional text after the closing double quotes and before the comma delimiter ({@code ,}); this
 *         additional text is treated as part of the field, following the rules for unquoted fields. For example:<br/>
 *         <samp>
 *             aaa,"bb"b,ccc CRLF ; interpreted as &lt;aaa&gt;, &lt;bbb&gt; and &lt;ccc&gt;<br/>
 *             xxx,"y"yy",zzz CRLF ; interpreted as &lt;xxx&gt;, &lt;yyy"&gt; and &lt;zzz&gt;
 *         </samp>
 *     </li>
 *     <li>
 *         <b>Termination of double quoted fields</b>: if a field starts with a double quote character ({@code "}) and
 *         the end of file is reached prior to the corresponding closing double quote, the field shall still be regarded
 *         as correctly terminated. For example:<br/>
 *         <samp>
 *             aaa,bbb,"ccc EOF ; interpreted as &lt;aaa&gt;, &lt;bbb&gt; and &lt;ccc&gt;<br/>
 *         </samp>
 *     </li>
 * </ul>
 */
public final class Rfc4180RelaxedFormat implements Format {
    private static final int BFL = 0;  // Before line
    private static final int BFF = 8;  // Before field
    private static final int SQT = 16; // Start quotation
    private static final int SQE = 24; // Escape at start quotation
    private static final int QOT = 32; // Quotation
    private static final int ESC = 40; // Escape
    private static final int FLD = 48; // Field
    private static final int CAR = 56; // Carriage return
    private static final int EOF = 64; // End of file
    private static final int ERR = 72; // Error

    // The parser is implemented with a finite state automaton in the form of a matrix. Each row is a state, and each
    // column is an input. A single cell encodes the next state and a set of actions. For example, being in state FLD
    // ("inside a field", 7th row) and receiving a comma (3rd column) gives the new state BFF ("before field") with
    // action EFH ("end field here"). Actions are defined in the Format interface, and are common to every Format
    // implementation. Clients use actions to construct the parsed output. In Java, an integer is 32 bits and the state
    // is encoded in the rightmost 16 bits, while the actions are encoded in the leftmost 16 bits.
    // The matrix contains padding elements at the end of each row and at the end of the body, so that rows are exactly
    // 8 elements long and the matrix is exactly 128 elements. This is for performance reasons. Refer to the consume()
    // method for an explanation.
    //@formatter:off
    private static final int[] DFA = {
    // *                "                ,                \r               \n               EOF
       FLD|SFH,         SQT,             BFF|SFH|EFH,     CAR|SFH|EFH,     BFL|SFH|EFH|ELH, EOF|STP,         0,0, // BFL
       FLD|SFH,         SQT,             BFF|SFH|EFH,     CAR|SFH|EFH,     BFL|SFH|EFH|ELH, EOF|SFH|EFH|STP, 0,0, // BFF
       QOT|SFH,         SQE,             QOT,             QOT,             QOT,             EOF|SFH|EFH|STP, 0,0, // SQT
       FLD|SFH,         QOT|SFH,         BFF|SFH|EFH,     CAR|SFH|EFH,     BFL|SFH|EFH,     EOF|SFH|EFH|STP, 0,0, // SQE
       QOT,             ESC,             QOT,             QOT,             QOT,             EOF|EFH|STP,     0,0, // QOT
       FLD|RCB,         QOT|RCB,         BFF|EFB,         CAR|EFB,         BFL|EFB|ELH,     EOF|EFB|STP,     0,0, // ESC
       FLD,             FLD,             BFF|EFH,         CAR|EFH,         BFL|EFH|ELH,     EOF|EFH|STP,     0,0, // FLD
       BFL|RLR,         BFL|ELH|RLR,     BFL|ELH|RLR,     BFL|ELH|RLR,     BFL|ELH,         EOF|STP,         0,0, // CAR
       ERR|ERH,         ERR|ERH,         ERR|ERH,         ERR|ERH,         ERR|ERH,         ERR|ERH,         0,0, // EOF
       ERR|ERH,         ERR|ERH,         ERR|ERH,         ERR|ERH,         ERR|ERH,         ERR|ERH,         0,0, // ERR
       0,               0,               0,               0,               0,               0,               0,0,
       0,               0,               0,               0,               0,               0,               0,0,
       0,               0,               0,               0,               0,               0,               0,0,
       0,               0,               0,               0,               0,               0,               0,0,
       0,               0,               0,               0,               0,               0,               0,0,
       0,               0,               0,               0,               0,               0,               0,0};
    //@formatter:on

    /**
     * Provider of {@link Rfc4180RelaxedFormat}.
     */
    public static final class Provider implements FormatProvider<Rfc4180RelaxedFormat> {
        /**
         * Return a new {@link Rfc4180RelaxedFormat} instance.
         *
         * @return A new {@link Rfc4180RelaxedFormat} instance.
         */
        @Override
        public Rfc4180RelaxedFormat provide() {
            return new Rfc4180RelaxedFormat();
        }
    }

    /**
     * Create a new {@code Rfc4180RelaxedFormat} instance.
     * <p>
     * This method is private, use {@link Provider#provide()} instead.
     */
    private Rfc4180RelaxedFormat() {
    }

    /**
     * {@inheritDoc}
     *
     * @return The initial state.
     */
    @Override
    @SuppressWarnings("Duplicates")
    public int base() {
        return BFL;
    }


    /**
     * Return the column index of the specified character in the matrix of the finite state automaton parser.
     *
     * @param c The character.
     * @return The column index of the specified character.
     */
    @SuppressWarnings("Duplicates")
    private int indexOf(int c) {
        // The following chain of if-statements is ordered by expected frequency, so that frequent cases appear first.
        // Doing so, we have significantly reduced the amount of computation necessary. For example, the comma character
        // might happen multiple times per line, so the corresponding if-statement appears before the new-line
        // character, which happens only once per line (in the general case). Another example is the new-line character
        // appearing before the carriage return character because CSV files often omit carriage return. The most
        // frequent case is a non-control character (such as 'a', the space or a digit) and  is covered by the first and
        // sixth conditions. The first condition checks if the input character is greater than comma. Comma is the CSV
        // control character with the highest ASCII value, so we know that anything above is a non-control. Control and
        // non-control characters are interleaved, so we need the 6th condition to select the remainder.
        if (c > ',') {
            return 0;
        } else if (c == ',') {
            return 2;
        } else if (c == '\n') {
            return 4;
        } else if (c == '\r') {
            return 3;
        } else if (c == '"') {
            return 1;
        } else if (c > 0) {
            return 0;
        } else { // EOF
            return 5;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param s The current state as returned by a previous call to {@link Format#base()} or this method.
     * @param c The character.
     * @return An integer encoding both the next state and the action to perform.
     */
    @Override
    @SuppressWarnings("Duplicates")
    public int consume(int s, int c) {
        // Given the current state and an input character, search the matrix for the next state and the actions to be
        // performed by the client when transitioning. This is a highly optimized implementation; a more naive
        // one would look like the following:
        //     return DFA[(s & 0xFFFF) + indexOf(c)];
        // The variable s encodes both state and the actions. The state resides in the rightmost 16 bits and corresponds
        // to the start index of the row in the finite state automaton matrix (0, 8, 16, and so on). It is returned by
        // the expression s & 0xFFFF, where the bit mask is applied to clean the leftmost 16 bits. Adding indexOf(c)
        // returns the cell containing the next state and transition actions.
        // The optimized implementation goes one step further. Each row in the finite state automaton is padded with
        // zeros on the right so to ensure each state contains exactly 8 elements. The first state in the matrix has
        // starting index 0 which in binary is 0000 0000, the second has index 8 which is 0000 1000, then 16 which is
        // 0001 0000, 24 which is 0001 1000, and so on. Note that since each state is a multiple of 8, the last three
        // bits are always zero. The method indexOf(c) returns an integer between 0 and 5 and such numbers are encoded
        // in binary with just 3 bits. So it's possible to effectively perform an addition using the bitwise operator |,
        // which might be faster than the + operator in some older architectures.
        // The automaton matrix only contains 80 cells, so we can use the smaller mask 0x7F instead of 0xFFFF to get the
        // state out. The automaton has been padded with zeros to the bottom until the size of 128, and by applying the
        // mask 0x7F (which is 127 in decimal) as the last operation of the expression, the Java JIT compiler should be
        // smart enough to realize that the index always falls inside the boundaries of the matrix and so eliminate the
        // cost of an array bound check.
        // The state FLD ("inside a field") is by far the most common, with a frequency of probably 90% or above. It
        // makes sense to hardcode this one specific value so to avoid paying for accessing the automaton matrix (which
        // is slow) and return a constant (which is fast).
        final int i = (s | indexOf(c)) & 0x7F;
        return i == FLD ? FLD : DFA[i];
    }
}
