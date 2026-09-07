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
 * CSV parsing implementations.
 * <ul>
 *   <li>
 *       {@link me.mneri.csv.parser.internal.SequentialLineParser}: A parser that processes CSV input one character at a
 *       time.
 *   </li>
 *   <li>
 *       {@link me.mneri.csv.parser.internal.SimdLineParser}: An optimized parser that leverages SIMD (Single
 *       Instruction, Multiple Data) operations to process multiple characters in parallel, providing superior
 *       performance on modern hardware.
 *   </li>
 * </ul>
 * <p>
 * <strong>Note:</strong> Classes in this package are internal implementation details and should not be used directly by
 * external code.</p>
 *
 * @author Massimo Neri &lt;<a href="mailto:hello@mneri.me">hello@mneri.me</a>&gt;
 */
package me.mneri.csv.parser.internal;