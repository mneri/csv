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

package me.mneri.csv.parser.internal;

/**
 * Parse the characters of a page into CSV lines.
 *
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public interface PageParser {
    /**
     * Parse the characters the page has just read, going on from where the previous call stopped: the parser keeps the
     * state of the format from one call to the next. The lines go into the page's table as they end; a line that runs
     * past the characters read stays unfinished, and the next call goes on with it. Parsing errors are recorded in the
     * page.
     * <p>
     * The parser reads where the new characters start from the page, rather than taking it as an argument: in our
     * measurements, the argument made the scalar parser 10% slower.
     *
     * @param page The page.
     */
    void parse(Page page);
}
