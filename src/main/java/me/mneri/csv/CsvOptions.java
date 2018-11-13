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

/**
 * Options for {@link CsvReader} and {@link CsvWriter}.
 *
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public class CsvOptions {
    private static final char DEFAULT_DELIMITER = ',';
    private static final char DEFAULT_QUOTATION = '"';

    private char delimiter = DEFAULT_DELIMITER;
    private char quotation = DEFAULT_QUOTATION;

    char getDelimiter() {
        return delimiter;
    }

    char getQuotation() {
        return quotation;
    }

    /**
     * Set the delimiter character used in reading or writing.
     *
     * @param delimiter the delimiter character.
     */
    public void setDelimiter(char delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * Set the quotation character used in reading or writing.
     *
     * @param quotation the quotation character.
     */
    public void setQuotation(char quotation) {
        this.quotation = quotation;
    }
}
