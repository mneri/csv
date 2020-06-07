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

package me.mneri.csv.exception;

import me.mneri.csv.reader.RecyclableCsvLine;

import java.util.List;

/**
 * This exception is thrown when an error occurs during the conversion process.
 *
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
public class CsvConversionException extends CsvException {
    public CsvConversionException(RecyclableCsvLine line, Throwable cause) {
        super(String.format("Error while converting values: %s", line.toString()), cause);
    }

    public CsvConversionException(List<String> line, Throwable cause) {
        super(String.format("Error while converting values: %s", line.toString()), cause);
    }
}
