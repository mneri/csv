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

/**
 * CSV format definitions and implementations.
 * <p>
 * This package provides a <strong>pluggable architecture</strong> for defining and implementing various CSV dialects
 * and standards. Each format encodes the rules for parsing CSV data, including field delimiters, quote characters,
 * escape sequences, line termination conventions, and error tolerance.
 * <p>
 * To understand the role of formats within the library, it helps to look at the collaboration between formats and
 * parsers:
 * <ul>
 *     <li>
 *         Formats are state machines; they update their state after consuming a character and return a list of actions
 *         to be performed, such as <i>start a field field</i>, or <i>end the field</i>.
 *     </li>
 *     <li>
 *         Parsers efficiently read characters from a stream, feed them to the format, and perform the actions dictated
 *         by the format.
 *     </li>
 * </ul>
 * This separation allows different CSV dialects to be plugged seamlessly into high-performance parsing pipelines
 * without duplicating stream-handling or optimization logic.
 *
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
package me.mneri.csv.format;