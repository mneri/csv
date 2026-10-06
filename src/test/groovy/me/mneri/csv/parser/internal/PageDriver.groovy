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

package me.mneri.csv.parser.internal

import java.nio.BufferOverflowException

/**
 * Parses a string with a {@link PageParser} a page at a time, the way the readers do: after each page, the line in
 * progress is carried over to the start of the next page, and parsing resumes where it stopped.
 * <p>
 * Empty fields are reported as empty strings.
 */
class PageDriver {
    private final PageParser parser

    PageDriver(PageParser parser) {
        this.parser = parser
    }

    /**
     * Return the lines of the input. Each page's lines are added to {@code out} before the page's error, if any, is
     * thrown; a line that fills a whole page throws {@link BufferOverflowException}, as {@link PageLoader} does.
     */
    List<List<String>> parse(String input, int pageSize, List<List<String>> out = []) {
        def reader = new StringReader(input)
        def page = new Page(pageSize)

        while (true) {
            page.carryover(page)
            page.fill(reader)
            parser.parse(page)

            for (int i = 0; i < page.lineCount(); i++) {
                def line = page.line(i)
                out << (0..<line.fieldCount).collect { line.getString(it) ?: "" }
            }
            if (page.error() != null) {
                throw page.error()
            }
            if (page.isLast()) {
                return out
            }
            if (page.tail == 0) {
                throw new BufferOverflowException()
            }
        }
    }
}
