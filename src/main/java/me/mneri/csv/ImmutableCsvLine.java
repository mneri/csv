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

import java.util.Arrays;

class ImmutableCsvLine extends RecyclableCsvLine {
    ImmutableCsvLine(RecyclableCsvLine source) {
        //@formatter:off
        super(Arrays.copyOf(source.chars,   source.getCharBufferLength()),
              Arrays.copyOf(source.endings, source.getEndingBufferLength()));
        //@formatter:on
    }

    void append(char c) {
        throw new UnsupportedOperationException();
    }

    void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    int getCharBufferLength() {
        return chars.length;
    }

    @Override
    int getEndingBufferLength() {
        return endings.length;
    }

    void markField() {
        throw new UnsupportedOperationException();
    }
}
