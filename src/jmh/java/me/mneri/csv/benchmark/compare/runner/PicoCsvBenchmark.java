package me.mneri.csv.benchmark.runner;

import me.mneri.csv.benchmark.compare.BenchmarkConstants;
import me.mneri.csv.benchmark.compare.BenchmarkState;
import nbbrd.picocsv.Csv;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
        List<String> fields = new ArrayList<>();

        try (Csv.Reader reader = Csv.Reader.of(Csv.Format.RFC4180, OPTIONS,
                new FileReader(state.file(), state.charset()))) {
            while (reader.readLine()) {
                fields.clear();
                while (reader.readField()) {
                    fields.add(reader.toString());
                }
                bh.consume(fields); // No domain translation, picocsv is already paying for an ArrayList
            }
        }
    }
}
