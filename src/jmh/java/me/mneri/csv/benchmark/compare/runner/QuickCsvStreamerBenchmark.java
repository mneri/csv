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
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import uk.elementarysoftware.quickcsv.api.CSVParser;
import uk.elementarysoftware.quickcsv.api.CSVParserBuilder;
import uk.elementarysoftware.quickcsv.api.CSVRecord;
import uk.elementarysoftware.quickcsv.api.Field;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@BenchmarkMode(Mode.AverageTime)
@Fork(BenchmarkConstants.FORKS)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = BenchmarkConstants.WARMUP_ITERATIONS)
@Measurement(iterations = BenchmarkConstants.MEASUREMENT_ITERATIONS)
public class QuickCsvStreamerBenchmark {
    @Benchmark
    public void run(BenchmarkState state, Blackhole bh) throws IOException {
        CSVParser<String[]> parser = CSVParserBuilder.<String[], NoHeader>aParser(QuickCsvStreamerBenchmark::toDomain)
                .forRfc4180()
                .usingCharset(state.charset())
                .build();

        try (Stream<String[]> rows = parser.parse(state.file()).sequential()) {
            rows.forEach(bh::consume);
        }
    }

    private static String[] toDomain(CSVRecord record) {
        String[] buffer = new String[16];
        int count = 0;
        Field field;
        while ((field = record.getNextField()) != null) {
            if (count == buffer.length) {
                buffer = Arrays.copyOf(buffer, count << 1);
            }
            buffer[count++] = field.asString();
        }
        return Arrays.copyOf(buffer, count);
    }

    private enum NoHeader {
    }
}