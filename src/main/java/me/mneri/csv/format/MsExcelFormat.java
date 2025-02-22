package me.mneri.csv.format;

import java.text.DecimalFormatSymbols;
import java.util.Locale;

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
 * This Microsoft Excel CSV variant allows for a locale-specific separator. When the locale's decimal separator is
 * period (as in Canada, UK and US), the CSV separator is comma ({@code ,}); when the locale's decimal
 * separator is comma (as in France, Germany and Italy), the CSV separator is semicolon ({@code ;}). For this reason,
 * constructing a new {@code MsExcelFormat} requires a {@link Locale} instance.
 * <p>
 * You can force a specific field delimiter by using the {@code sep} delimiter specification. This specification should
 * be placed at the very top of your CSV file as in the following example:<br/>
 * <samp>
 *     sep=^ CRLF<br/>
 *     aaa^bbb^ccc CRLF<br/>
 *     xxx^yyy^zzz CRLF
 * </samp>
 */
public final class MsExcelFormat implements Format {
    private static final int BFL = 0; // Before line
    private static final int BFF = 8; // Before field
    private static final int SQT = 16; // Start quotation
    private static final int SQE = 24; // Escape at start quotation
    private static final int QOT = 32; // Quotation
    private static final int ESC = 40; // Escape
    private static final int FLD = 48; // Field
    private static final int CAR = 56; // Carriage return
    private static final int EOF = 64; // End of file
    private static final int ERR = 72; // End of file
    private static final int DS1 = 80;
    private static final int DS2 = 96;
    private static final int DS3 = 112;
    private static final int DS4 = 128;
    private static final int DS5 = 144;
    private static final int DS6 = 160;
    private static final int DS7 = 176;

    private static final int HLD = 1 << 31; // Hold
    private static final int STR = 1 << 30; // Store
    private static final int EDS = 1 << 29; // Stop DS

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
    // *                "                ,                \r               \n               EOF
    // s                e                p                =
       FLD|SFH|EDS,     SQT|EDS,         BFF|SFH|EFH|EDS, CAR|SFH|EFH|EDS, BFL|SFH|EFH|ELH|EDS, EOF|STP|EDS, 0,0, // DS1
       DS2|SFH,         FLD|SFH|EDS,     FLD|SFH|EDS,     FLD|SFH|EDS,     0,               0,               0,0,
       FLD|EDS,         FLD|EDS,         BFF|EFH|EDS,     CAR|EFH|EDS,     BFL|EFH|ELH|EDS, EOF|EFH|STP|EDS, 0,0, // DS2
       FLD|EDS,         DS3,             FLD|EDS,         FLD|EDS,         0,               0,               0,0,
       FLD|EDS,         FLD|EDS,         BFF|EFH|EDS,     CAR|EFH|EDS,     BFL|EFH|ELH|EDS, EOF|EFH|STP|EDS, 0,0, // DS3
       FLD|EDS,         FLD|EDS,         DS4,             FLD|EDS,         0,               0,               0,0,
       FLD|EDS,         FLD|EDS,         BFF|EFH|EDS,     CAR|EFH|EDS,     BFL|EFH|ELH|EDS, EOF|EFH|STP|EDS, 0,0, // DS4
       FLD|EDS,         FLD|EDS,         FLD|EDS,         DS5,             0,               0,               0,0,
       DS6|HLD,         FLD|EDS,         DS6|HLD,         CAR|EFH|EDS,     BFL|EFH|ELH|EDS, EOF|EFH|STP|EDS, 0,0, // DS5
       DS6|HLD,         DS6|HLD,         DS6|HLD,         DS6|HLD,         0,               0,               0,0,
       FLD|EDS,         FLD|EDS,         BFF|EFH|EDS,     DS7,             BFL|STR|EDS,     EOF|STP|EDS,     0,0, // DS6
       FLD|EDS,         FLD|EDS,         FLD|EDS,         FLD|EDS,         0,               0,               0,0,
       BFL|RLR|STR|EDS, BFL|RLR|STR|EDS, BFL|RLR|STR|EDS, BFL|RLR|STR|EDS, BFL|STR|EDS,     EOF|STP|STR|EDS, 0,0, // DS7
       BFL|RLR|STR|EDS, BFL|RLR|STR|EDS, BFL|RLR|STR|EDS, BFL|RLR|STR|EDS, 0,               0,               0,0};
    //@formatter:on

    public static final class Provider implements FormatProvider<MsExcelFormat> {
        private final Locale locale;

        public Provider(Locale locale) {
            this.locale = locale;
        }

        @Override
        public MsExcelFormat provide() {
            return new MsExcelFormat(locale);
        }
    }

    private boolean isDs = true;
    private int hold;
    private int delim;

    private MsExcelFormat(Locale locale) {
        this.delim = DecimalFormatSymbols.getInstance(locale).getDecimalSeparator() == ',' ? ';' : ',';
    }

    /**
     * {@inheritDoc}
     *
     * @return The initial state.
     */
    @Override
    public int base() {
        return isDs ? DS1 : BFL;
    }

    private int indexOf(int c) {
        if (c == delim) {
            return 2;
        } else if (c > '"') {
            return 0;
        } else if (c == '\n') {
            return 4;
        } else if (c == '\r') {
            return 3;
        } else if (c == '"') {
            return 1;
        } else if (c > 0) {
            return 0;
        }
        return 5;
    }

    private int indexOfDs(int c) {
        if (c == 's') {
            return 8;
        } else if (c == 'e') {
            return 9;
        } else if (c == 'p') {
            return 10;
        } else if (c == '=') {
            return 11;
        } else if (c == '\n') {
            return 4;
        } else if (c == '\r') {
            return 3;
        } else if (c == '"') {
            return 1;
        } else if (c > 0) {
            return 0;
        }
        return 5;
    }

    /**
     * {@inheritDoc}
     *
     * @param s The current state as returned by a previous call to {@link Format#base()} or this method.
     * @param c The character.
     * @return An integer encoding both the next state and the action to perform.
     */
    @Override
    public int consume(int s, int c) {
        if (isDs) {
            return consumeDs(s, c);
        } else {
            final int i = (s | indexOf(c)) & 0x7F;
            return i == FLD ? FLD : DFA[i];
        }
    }

    private int consumeDs(int s, int c) {
        int t = DFA[(s | indexOfDs(c)) & 0xFF];
        if ((t & EDS) != 0) {
            isDs = false;
        }
        if ((t & HLD) != 0) {
            hold = c;
        }
        if ((t & STR) != 0) {
            delim = hold;
        }
        return t;
    }
}
