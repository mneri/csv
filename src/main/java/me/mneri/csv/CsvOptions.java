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
    private static final int DEFAULT_MAX_LINE_LENGTH = 16386;

    private int delimiter;
    private int maxLineLength;
    private int quotation;

    private CsvOptions() {
        maxLineLength = DEFAULT_MAX_LINE_LENGTH;
    }

    void check() {
        //@formatter:off
        if     (delimiter == '\r' || delimiter == '\n' || delimiter <= 0 ||
                quotation == '\r' || quotation == '\n' || quotation <= 0 ||
                delimiter == quotation || maxLineLength <= 0) {
            throw new IllegalCsvOptionsException();
        }
        //@formatter:on
    }

    /**
     * Get default options.
     *
     * @return A {@code CsvOptions} object initialized with default values.
     */
    public static CsvOptions defaultOptions() {
        return rfc4180();
    }

    /**
     * Get MS Excel options.
     *
     * @return A {@code CsvOptions} object initialized with MS Excel values.
     */
    public static CsvOptions excel() {
        CsvOptions options = new CsvOptions();
        options.setDelimiter(';');
        options.setQuotation('"');
        return options;
    }

    int getDelimiter() {
        return delimiter;
    }

    int getMaxLineLength() {
        return maxLineLength;
    }

    int getQuotation() {
        return quotation;
    }

    /**
     * Get RFC 4180 options.
     *
     * @return A {@code CsvOptions} object initialized with RFC 4180 values.
     */
    public static CsvOptions rfc4180() {
        CsvOptions options = new CsvOptions();
        options.setDelimiter(',');
        options.setQuotation('"');
        return options;
    }

    /**
     * Set the delimiter character used in reading or writing.
     *
     * @param delimiter the delimiter character.
     */
    public void setDelimiter(int delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * Set the maximum number of characters per line the parser is allowed to read.
     * <p>
     * Avoid {@link OutOfMemoryError}s that could be thrown in case the file is not the proper format.
     *
     * @param maxLineLength The maximum length in number of characters.
     */
    public void setMaxLineLength(int maxLineLength) {
        this.maxLineLength = maxLineLength;
    }

    /**
     * Set the quotation character used in reading or writing.
     *
     * @param quotation the quotation character.
     */
    public void setQuotation(int quotation) {
        this.quotation = quotation;
    }
}
