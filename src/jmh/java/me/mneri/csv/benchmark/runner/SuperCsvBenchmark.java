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

package me.mneri.csv.benchmark.runner;

import me.mneri.csv.benchmark.BenchmarkConstants;
import me.mneri.csv.benchmark.BenchmarkState;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@Fork(BenchmarkConstants.FORKS)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = BenchmarkConstants.WARMUP_ITERATIONS)
@Measurement(iterations = BenchmarkConstants.MEASUREMENT_ITERATIONS)
public class SuperCsvBenchmark {
    @Benchmark
    public void run(BenchmarkState state, Blackhole bh) throws IOException {
        try (CsvListReader reader = new CsvListReader(new FileReader(state.file(), state.charset()), CsvPreference.STANDARD_PREFERENCE)) {
            List<String> next;
            while ((next = reader.read()) != null) {
                bh.consume(toDomain(next));
            }
        }
    }

    private String[] toDomain(List<String> in) {
        return in.toArray(new String[0]);
    }
}