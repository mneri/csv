package me.mneri.csv.format;

import java.util.List;

/**
 * A specific CSV dialect.
 * <p>
 * Implementors of this interface have to obey to the following contract:
 * <ul>
 *     <li>
 *         Return a start of field marker at the start of the field. For unquoted fields, at the
 *         first character; for quoted fields, at the first character after the quotation symbol; in case of escape
 *         at the beginning of a quoted field, return the start of field marker after the escape character.
 *         For example:<br/>
 *         <samp>
 *             aaa,"bbb","""ccc"""\r\n
 *         </samp>
 *     </li>
 *     <li>
 *         Either return end of field marker or end of field marker before, never both.
 *     </li>
 *     <li>
 *         Return the end of field marker on the character immediately following the last character of the field.
 *     </li>
 *     <li>
 *         Return the end of field before marker at the second character following the last character of the field. This
 *         is useful in cases where it's not possible to determine if the current character is the last of a field or
 *         not and we need a lookahead.
 *     </li>
 *     <li>
 *         Return the remove character before marker when the previous character needs to be removed from the field.
 *     </li>
 *     <li>
 *         Return the repeat last read character when it's required the client submits the same character as before.
 *         This is useful in cases where it's not possible to determine the end of the line at the current position
 *         and we need a lookahead.
 *     </li>
 *     <li>
 *          Return the end of the line marker strictly before the first character of the following line.
 *     </li>
 *     <li>
 *         Return the stop marker at the end of file.
 *     </li>
 *     <li>
 *         Return the err marker when you encounter an error in the format of the CSV file.
 *     </li>
 * </ul>
 */
public interface Format {
    int SFH = 1 << 16; // Start field at the current position.
    int EFH = 1 << 17; // End field at the current position.
    int EFB = 1 << 18; // End field at the previous position.
    int ELH = 1 << 19; // End line at the current position.
    int RLR = 1 << 20; // Repeat last read.
    int RCB = 1 << 21; // Remove the character at the previous position.
    int STP = 1 << 22; // Stop processing the current line.
    int ERH = 1 << 23; // Error at the current position.

    int ANY = 0x0FFF << 16;


    /**
     * Return the initial state.
     *
     * @return An integer encoding the initial state.
     */
    int base();

    /**
     * Given the current state and a character, return an integer encoding both the next state and the action to
     * perform.
     *
     * @param s The current state as returned by a previous call to {@link Format#base()} or this method.
     * @param c The character.
     * @return An integer encoding both the next state and the action to perform.
     */
    int consume(int s, int c);
}
