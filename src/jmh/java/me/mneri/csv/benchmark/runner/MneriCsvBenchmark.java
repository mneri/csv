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

import me.mneri.csv.CsvReader;
import me.mneri.csv.benchmark.BenchmarkState;
import me.mneri.csv.benchmark.BenchmarkConstants;
import me.mneri.csv.deserializer.StringArrayDeserializer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = BenchmarkConstants.WARMUP_ITERATIONS)
@Measurement(iterations = BenchmarkConstants.MEASUREMENT_ITERATIONS)
public abstract class MneriCsvBenchmark {
    @Benchmark
    public void run(BenchmarkState state, Blackhole bh) throws IOException {
        try (CsvReader<String[]> reader = CsvReader.open(state.file(), state.charset(), new StringArrayDeserializer())) {
            while (reader.hasNext()) {
                bh.consume(reader.next());
            }
        }
    }

    @Fork(BenchmarkConstants.FORKS)
    public static class Sequential extends MneriCsvBenchmark {
    }

    @Fork(value = BenchmarkConstants.FORKS, jvmArgsPrepend = "--add-modules=jdk.incubator.vector")
    public static class Vector extends MneriCsvBenchmark {
    }
}