package me.mneri.csv.format;

/**
 * Implements the interpretation of CSV files performed by <a href="https://pandas.pydata.org">pandas</a>'
 * {@code pandas.read_csv()} using its default, C-based parsing engine ({@code engine='c'}). Unlike
 * {@link Rfc4180FullyRelaxedFormat}, this format is <b>not</b> guaranteed to never emit errors: several
 * malformations that Excel-style parsers recover from are treated as hard errors here.
 * <p>
 * The following features and peculiarities are supported (verified against pandas 3.0.2):
 * <ul>
 *     <li>
 *         <b>Variable number of fields, in one direction only</b>: a row with <i>fewer</i> fields than the field
 *         count established by the first row is accepted, with the missing trailing fields treated as null. A row
 *         with <i>more</i> fields than the established count is a hard error, unless the implicit-index heuristic
 *         below applies. For example:<br/>
 *         <samp>
 *             a,b,c CRLF<br/>
 *             x,y CRLF ; interpreted as &lt;x&gt;, &lt;y&gt; and &lt;null&gt;<br/>
 *         </samp>
 *         but:<br/>
 *         <samp>
 *             a,b,c CRLF<br/>
 *             x,y,z,w CRLF ; raises: "Expected 3 fields in line 2, saw 4"<br/>
 *         </samp>
 *     </li>
 *     <li>
 *         <b>Implicit index column</b>: when the header row is inferred (the default; not when the header is
 *         explicitly disabled) and every data row consistently has exactly one more field than the header, the
 *         parser silently treats the leftmost, unnamed extra field as a row index rather than raising an error, even
 *         though it would otherwise be a "too many fields" mismatch. This heuristic is evaluated once, from the
 *         shape of the data, and then applied uniformly to the rest of the file. For example:<br/>
 *         <samp>
 *             a,b,c CRLF<br/>
 *             idx1,1,2,3 CRLF ; interpreted as row index &lt;idx1&gt;, columns &lt;1&gt;,&lt;2&gt;,&lt;3&gt;<br/>
 *             idx2,4,5,6 CRLF
 *         </samp>
 *         This heuristic does not activate at all when the header is explicitly turned off; the same input then
 *         falls back to the ordinary "too many fields" error above.
 *     </li>
 *     <li>
 *         <b>Line termination</b>: lines can end with {@code \r\n}, {@code \r}, or {@code \n}; files can be
 *         inconsistent in their line termination, using different line terminators on different lines, in any
 *         order, any number of times — this matches {@link Rfc4180FullyRelaxedFormat} exactly. All seven
 *         combinations of consecutive line pairs (LF/LF, CRLF/CRLF, CR/CR, CRLF/LF, LF/CRLF, CR/LF, LF/CR) parse
 *         identically. A line terminator occurring inside a quoted field (of any of the three kinds) is treated as
 *         literal field content, never as a row terminator.
 *     </li>
 *     <li>
 *         <b>Fields containing double quotes</b>: fields that do not begin with a double quote character
 *         ({@code "}) may contain double quotes anywhere within them; such quotes are treated as ordinary
 *         characters. For example:<br/>
 *         <samp>
 *             aaa,b"b"b,ccc CRLF ; interpreted as &lt;aaa&gt;, &lt;b"b"b&gt; and &lt;ccc&gt;<br/>
 *             xxx,y"y,zzz CRLF   ; interpreted as &lt;xxx&gt;, &lt;y"y&gt; and &lt;zzz&gt;
 *         </samp>
 *     </li>
 *     <li>
 *         <b>Extra text after a double quoted field</b>: fields that begin with a double quote character may
 *         include additional text after the closing double quote and before the delimiter; this additional text is
 *         treated as part of the field, following the rules for unquoted fields — matching
 *         {@link Rfc4180FullyRelaxedFormat} exactly, including its re-entry rule: a double quote immediately
 *         following the closing quote (before any other character) is interpreted as an RFC4180-style escaped
 *         literal quote and re-opens quoted content; once any other character has intervened, subsequent quotes are
 *         purely literal and never re-open quoted mode. For example:<br/>
 *         <samp>
 *             aaa,"bb"b,ccc CRLF        ; interpreted as &lt;aaa&gt;, &lt;bbb&gt; and &lt;ccc&gt;<br/>
 *             xxx,"y"yy",zzz CRLF       ; interpreted as &lt;xxx&gt;, &lt;yyy"&gt; and &lt;zzz&gt;<br/>
 *             aaa,"bb""cc",ddd CRLF     ; interpreted as &lt;aaa&gt;, &lt;bb"cc&gt; and &lt;ddd&gt;<br/>
 *             aaa,"bb"b"cc",ddd CRLF    ; interpreted as &lt;aaa&gt;, &lt;bbb"cc"&gt; and &lt;ddd&gt;<br/>
 *             aaa,"bb"x""cc",ddd CRLF   ; interpreted as &lt;aaa&gt;, &lt;bbx""cc"&gt; and &lt;ddd&gt;
 *         </samp>
 *     </li>
 *     <li>
 *         <b>Termination of double quoted fields is <i>not</i> relaxed</b>: unlike
 *         {@link Rfc4180FullyRelaxedFormat}, if a field starts with a double quote and the end of file is reached
 *         before the corresponding closing quote, this is a hard error in every case tested (with or without a
 *         trailing line terminator, and whether or not the malformed field is on the first line of the file). For
 *         example:<br/>
 *         <samp>
 *             aaa,bbb,"ccc EOF ; raises: "EOF inside string starting at row 0"<br/>
 *         </samp>
 *     </li>
 *     <li>
 *         <b>Comment lines</b> (when a comment character is configured, e.g. {@code '#'}): everything from the
 *         first unquoted occurrence of the comment character to the end of the line is discarded, character by
 *         character — including mid-token, not only at a field or line boundary. Occurrences of the comment
 *         character <i>inside a quoted field</i> are correctly recognised as literal data and are not treated as a
 *         comment start. A line is only fully skipped (rather than kept as a truncated, possibly null-padded row) if
 *         the comment character is the very first character of the line; leading whitespace before the comment
 *         character defeats this and the line is instead kept as a short data row. For example, with comment
 *         character {@code '#'}:<br/>
 *         <samp>
 *             a,b,c CRLF<br/>
 *             #this is a comment CRLF ; entire line skipped<br/>
 *             x,y,z CRLF<br/>
 *             a,"b#c",d CRLF ; interpreted as &lt;a&gt;, &lt;b#c&gt; and &lt;d&gt; (# inside quotes is literal)<br/>
 *             ab#cd,ef,gh CRLF ; interpreted as the single field &lt;ab&gt; (comment truncates mid-token)<br/>
 *             {@code "   #comment"} CRLF ; NOT skipped: kept as a row whose first field is {@code "   "}
 *         </samp>
 *     </li>
 *     <li>
 *         <b>Blank lines</b> are skipped by default (equivalent to {@code skip_blank_lines=true}); this can be
 *         disabled, in which case a blank line becomes a row of nulls rather than being dropped.
 *     </li>
 * </ul>
 */
public class PandasCFormat {
}
