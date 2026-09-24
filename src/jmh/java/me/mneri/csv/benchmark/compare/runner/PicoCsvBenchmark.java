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

package me.mneri.csv.benchmark.compare.runner;

import me.mneri.csv.benchmark.compare.BenchmarkConstants;
import me.mneri.csv.benchmark.compare.BenchmarkState;
import nbbrd.picocsv.Csv;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@Fork(BenchmarkConstants.FORKS)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = BenchmarkConstants.WARMUP_ITERATIONS)
@Measurement(iterations = BenchmarkConstants.MEASUREMENT_ITERATIONS)
public class PicoCsvBenchmark {
    private static final Csv.ReaderOptions OPTIONS = Csv.ReaderOptions.DEFAULT.toBuilder()
            .lenientSeparator(true)
            .build();

    @Benchmark
    public void run(BenchmarkState state, Blackhole bh) throws IOException {
        String[] buffer = new String[16]; // Grows if a line has more fields; reused across lines

        try (Csv.Reader reader = Csv.Reader.of(Csv.Format.RFC4180, OPTIONS,
                new FileReader(state.file(), state.charset()))) {
            while (reader.readLine()) {
                int count = 0;
                while (reader.readField()) {
                    // picocsv can't infer the number of fields. To keep things as fair as possible, we collect the
                    // fields in a growing array, and we create a new final array per row.
                    if (count == buffer.length) {
                        buffer = Arrays.copyOf(buffer, count << 1);
                    }
                    buffer[count++] = reader.toString();
                }
                bh.consume(Arrays.copyOf(buffer, count));
            }
        }
    }
}
