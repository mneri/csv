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

package me.mneri.csv;

final class TextUtil {
    //@formatter:off
    private static final String[] NON_PRINTABLE =
            { "{NUL}", "{SOH}", "{STX}", "{ETX}", "{EOT}", "{ENQ}", "{ACK}", "{BEL}",
              "{BS}" , "{TAB}", "{LF}" , "{VT}" , "{FF}" , "{CR}" , "{SO}" , "{SI}" ,
              "{DLE}", "{DC1}", "{DC2}", "{DC3}", "{DC4}", "{NAK}", "{SYN}", "{ETB}",
              "{CAN}", "{EM}" , "{SUB}", "{ESC}", "{FS}" , "{GS}" , "{RS}" , "{US}"   };
    //@formatter:on

    private TextUtil() {
    }

    static String printable(int c) {
        //@formatter:off
        if      (c == -1) { return "{EOF}"; }
        else if (c < 32)  { return NON_PRINTABLE[c]; }
        else              { return String.valueOf((char) c); }
        //@formatter:on
    }
}
