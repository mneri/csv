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
import me.mneri.csv.concurrent.DefaultThreadFactory;
import me.mneri.csv.deserializer.StringArrayDeserializer;
import me.mneri.csv.format.Rfc4180FullyRelaxedFormat;
import me.mneri.csv.reader.CsvReader;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.concurrent.TimeUnit;

// Scalar forks don't load the Vector API module, so the reader falls back to the scalar parser.
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = BenchmarkConstants.WARMUP_ITERATIONS)
@Measurement(iterations = BenchmarkConstants.MEASUREMENT_ITERATIONS)
public abstract class MneriCsvBenchmark {
    @Fork(BenchmarkConstants.FORKS)
    public static class SequentialScalar extends MneriCsvBenchmark {
        @Benchmark
        public void run(BenchmarkState state, Blackhole bh) throws IOException {
            Reader in = new InputStreamReader(new FileInputStream(state.file()), state.charset());
            try (CsvReader<String[]> reader = CsvReader.open(
                    in, Rfc4180FullyRelaxedFormat.provider(), new StringArrayDeserializer())) {
                while (reader.hasNext()) {
                    bh.consume(reader.next());
                }
            }
        }
    }

    @Fork(value = BenchmarkConstants.FORKS, jvmArgsPrepend = "--add-modules=jdk.incubator.vector")
    public static class SequentialVector extends MneriCsvBenchmark {
        @Benchmark
        public void run(BenchmarkState state, Blackhole bh) throws IOException {
            Reader in = new InputStreamReader(new FileInputStream(state.file()), state.charset());
            try (CsvReader<String[]> reader = CsvReader.open(
                    in, Rfc4180FullyRelaxedFormat.provider(), new StringArrayDeserializer())) {
                while (reader.hasNext()) {
                    bh.consume(reader.next());
                }
            }
        }
    }

    @Fork(BenchmarkConstants.FORKS)
    public static class ParallelScalar extends MneriCsvBenchmark {
        @Benchmark
        public void run(BenchmarkState state, Blackhole bh) throws IOException {
            Reader in = new InputStreamReader(new FileInputStream(state.file()), state.charset());
            try (CsvReader<String[]> reader = CsvReader.parallel(
                    new DefaultThreadFactory(), in, Rfc4180FullyRelaxedFormat.provider(),
                    new StringArrayDeserializer())) {
                while (reader.hasNext()) {
                    bh.consume(reader.next());
                }
            }
        }
    }

    @Fork(value = BenchmarkConstants.FORKS, jvmArgsPrepend = "--add-modules=jdk.incubator.vector")
    public static class ParallelVector extends MneriCsvBenchmark {
        @Benchmark
        public void run(BenchmarkState state, Blackhole bh) throws IOException {
            Reader in = new InputStreamReader(new FileInputStream(state.file()), state.charset());
            try (CsvReader<String[]> reader = CsvReader.parallel(
                    new DefaultThreadFactory(), in, Rfc4180FullyRelaxedFormat.provider(),
                    new StringArrayDeserializer())) {
                while (reader.hasNext()) {
                    bh.consume(reader.next());
                }
            }
        }
    }
}
