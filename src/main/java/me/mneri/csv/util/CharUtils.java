/*
 * This file is part of mneri/csv.
 *
 * mneri/csv is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mneri/csv is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with mneri/csv. If not, see <http://www.gnu.org/licenses/>.
 */

package me.mneri.csv.util;

public final class CharUtils {
    //@formatter:off
    private static final String[] NON_PRINTABLE =
            { "{NUL}", "{SOH}", "{STX}", "{ETX}", "{EOT}", "{ENQ}", "{ACK}", "{BEL}",
              "{BS}" , "{TAB}", "{LF}" , "{VT}" , "{FF}" , "{CR}" , "{SO}" , "{SI}" ,
              "{DLE}", "{DC1}", "{DC2}", "{DC3}", "{DC4}", "{NAK}", "{SYN}", "{ETB}",
              "{CAN}", "{EM}" , "{SUB}", "{ESC}", "{FS}" , "{GS}" , "{RS}" , "{US}"   };
    //@formatter:on

    private CharUtils() {
    }

    public static String printable(int c) {
        //@formatter:off
        if      (c == -1) { return "{EOF}"; }
        else if (c < 32)  { return NON_PRINTABLE[c]; }
        else              { return String.valueOf((char) c); }
        //@formatter:on
    }
}
