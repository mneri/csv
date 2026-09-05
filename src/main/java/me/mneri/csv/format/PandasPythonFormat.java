package me.mneri.csv.format;

/**
 * Implements the interpretation of CSV files performed by <a href="https://pandas.pydata.org">pandas</a>'
 * {@code pandas.read_csv()} using its pure-Python fallback engine ({@code engine='python'}). This engine is
 * generally less permissive than {@link PandasCFormat} and diverges from it on several concrete edge cases; those
 * divergences are called out explicitly below. Like {@link PandasCFormat}, this format is <b>not</b> guaranteed to
 * never emit errors.
 * <p>
 * The following features and peculiarities are supported (verified against pandas 3.0.2):
 * <ul>
 *     <li>
 *         <b>Variable number of fields, in one direction only</b>: identical behaviour to {@link PandasCFormat} —
 *         rows with fewer fields than the established count are null-padded; rows with more fields raise an error
 *         (message text differs slightly from the C engine's, e.g. {@code "Expected 3 fields in line 2, saw 4"}),
 *         unless the implicit-index heuristic below applies.
 *     </li>
 *     <li>
 *         <b>Implicit index column</b>: identical heuristic and behaviour to {@link PandasCFormat}: when the header
 *         is inferred and every data row has exactly one more field than the header, the leftmost extra field
 *         becomes the row index instead of raising an error. Disabled when the header is explicitly turned off.
 *     </li>
 *     <li>
 *         <b>Line termination is only partially supported</b>: {@code \n} and {@code \r\n} line terminators work
 *         correctly and can be mixed within the same file. A bare {@code \r} used as the <i>sole</i> line terminator
 *         is <b>not</b> reliably supported: encountering a lone {@code \r} before any {@code \n} has been seen in
 *         the stream raises an error, even though the identical file parses correctly under {@link PandasCFormat}.
 *         For example:<br/>
 *         <samp>
 *             a,b,c CR<br/>
 *             x,y,z CR ; raises: "new-line character seen in unquoted field - do you need to open the file
 *             with newline=''?"
 *         </samp>
 *         A line terminator occurring inside a quoted field (of any of the three kinds) is still treated as literal
 *         field content, never as a row terminator, exactly as in {@link PandasCFormat}.
 *     </li>
 *     <li>
 *         <b>Fields containing double quotes</b>: identical to {@link PandasCFormat} — a double quote inside a
 *         field that does not begin with one is treated as an ordinary character. For example:<br/>
 *         <samp>
 *             aaa,b"b"b,ccc CRLF ; interpreted as &lt;aaa&gt;, &lt;b"b"b&gt; and &lt;ccc&gt;
 *         </samp>
 *     </li>
 *     <li>
 *         <b>Extra text after a double quoted field is <i>not</i> supported</b>, except for the single specific case
 *         of a double quote immediately following the closing quote (the standard RFC4180 {@code ""} escaped-quote
 *         sequence), which is accepted identically to {@link PandasCFormat}. Any other extra text — a bare
 *         character, or a third or fourth consecutive quote — after a closing quote is a hard error, unlike
 *         {@link PandasCFormat}, which tolerates and interprets all of these. For example:<br/>
 *         <samp>
 *             aaa,"bb""cc",ddd CRLF   ; interpreted as &lt;aaa&gt;, &lt;bb"cc&gt; and &lt;ddd&gt; (accepted)<br/>
 *             aaa,"bb"b,ccc CRLF      ; raises: "',' expected after '"'" (rejected — unlike {@link PandasCFormat})<br/>
 *             aaa,"bb"""cc",ddd CRLF  ; raises: "',' expected after '"'" (rejected — unlike {@link PandasCFormat})
 *         </samp>
 *     </li>
 *     <li>
 *         <b>Termination of double quoted fields is <i>not</i> relaxed</b>: identical to {@link PandasCFormat} in
 *         effect — an unclosed quoted field at end of file is a hard error — though the error message differs
 *         (e.g. {@code "unexpected end of data"} rather than the C engine's {@code "EOF inside string..."}).
 *     </li>
 *     <li>
 *         <b>Comment lines are not quote-aware — this is a bug-like divergence from {@link PandasCFormat}</b>: the
 *         comment character is recognised and truncates the rest of the line from its first occurrence just as in
 *         {@link PandasCFormat}, but unlike the C engine, an occurrence <i>inside a quoted field</i> is
 *         <b>incorrectly</b> treated as a comment start too, silently corrupting quoted data that happens to contain
 *         the comment character. For example, with comment character {@code '#'}:<br/>
 *         <samp>
 *             a,"b#c",d CRLF ; interpreted as the single field &lt;a&gt; (data after the {@code #} is lost —
 *             {@link PandasCFormat} correctly keeps &lt;b#c&gt; as the second field)
 *         </samp>
 *         Conversely, this engine is <i>more</i> lenient than {@link PandasCFormat} regarding leading whitespace: a
 *         line consisting of whitespace followed by the comment character is skipped in its entirety, whereas
 *         {@link PandasCFormat} keeps such a line as a short, non-comment data row.
 *     </li>
 *     <li>
 *         <b>Blank lines</b> are skipped by default, identically to {@link PandasCFormat}.
 *     </li>
 * </ul>
 */
public class PandasPythonFormat {
}
