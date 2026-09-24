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

package me.mneri.csv.format

import me.mneri.csv.exception.UnexpectedCharacterException

import static me.mneri.csv.format.Format.*

class FormatDriver {
    private final Format format

    FormatDriver(Provider<? extends Format> provider) {
        this.format = provider.provide()
    }

    List<List<String>> parse(String input) throws UnexpectedCharacterException {
        List<List<String>> result = []
        List<String> line = null

        int pos = 0
        int start = 0
        int s = format.base()

        while (pos <= input.length()) { // Loop one over to add the EOF character
            int c = (pos < input.length()) ? ((int) input.charAt(pos)) : -1
            s = format.consume(s, c)

            if ((s & SFH) != 0) {
                start = pos
            }
            if ((s & EFH) != 0) {
                if (line == null) {
                    line = []
                }
                line.add(input.substring(start, pos))
            }
            if ((s & EFB) != 0) {
                if (line == null) {
                    line = []
                }
                line.add(input.substring(start, pos - 1))
            }
            if ((s & (ELH | ELB)) != 0) {
                result.add(line)
                line = null
            }
            if ((s & RPL) != 0) {
                pos = pos - 1
            }
            if ((s & RMB) != 0) {
                input = input.substring(0, pos - 1) + input.substring(pos)
                pos = pos - 1
            }
            if ((s & ERH) != 0) {
                throw new UnexpectedCharacterException(result.size())
            }
            if ((s & STP) != 0) {
                break
            }
            pos = pos + 1
        }

        if (line != null) {
            result.add(line)
        }

        return result
    }
}