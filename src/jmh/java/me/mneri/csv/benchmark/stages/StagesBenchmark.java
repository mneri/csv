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

package me.mneri.csv.benchmark.stages;

import me.mneri.csv.benchmark.compare.BenchmarkConstants;
import me.mneri.csv.benchmark.compare.BenchmarkState;
import me.mneri.csv.deserializer.StringArrayDeserializer;
import me.mneri.csv.parser.RecycledLine;
import me.mneri.csv.reader.CsvReader;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.concurrent.TimeUnit;

// Each benchmark adds a stage to the previous one: parsing costs parse - read, and object creation costs
// parseAndDeserialize - parse.
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = BenchmarkConstants.WARMUP_ITERATIONS)
@Measurement(iterations = BenchmarkConstants.MEASUREMENT_ITERATIONS)
@Fork(value = BenchmarkConstants.FORKS, jvmArgsPrepend = "--add-modules=jdk.incubator.vector")
public class StagesBenchmark {
    @Benchmark
    public long read(BenchmarkState state) throws IOException {
        char[] buf = new char[4_096];
        long count = 0;
        try (Reader reader = reader(state)) {
            int read;
            while ((read = reader.read(buf)) >= 0) {
                count += read;
            }
        }
        return count;
    }

    @Benchmark
    public void parse(BenchmarkState state, Blackhole bh) throws IOException {
        consume(CsvReader.open(reader(state), RecycledLine::getFieldCount), bh);
    }

    @Benchmark
    public void parseAndDeserialize(BenchmarkState state, Blackhole bh) throws IOException {
        consume(CsvReader.open(reader(state), new StringArrayDeserializer()), bh);
    }

    void consume(CsvReader<?> reader, Blackhole bh) throws IOException {
        try (reader) {
            while (reader.hasNext()) {
                bh.consume(reader.next());
            }
        }
    }

    Reader reader(BenchmarkState state) throws IOException {
        return new InputStreamReader(new FileInputStream(state.file()), state.charset());
    }
}
