package me.mneri.csv.format;

import me.mneri.csv.exception.CsvException;
import me.mneri.csv.reader.CsvReader;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class CsvReaderBenchmark {
    private File file;

    @Setup(Level.Invocation)
    public void beforeEach() throws IOException {
        file = File.createTempFile("benchmark", ".csv");
        try (FileWriter writer = new FileWriter(file)) {
            for (int i = 0; i < 163_840; i++) {
                writer.write("12,4567890,,\"456789012345678\",123456789,123456789,123456789,12\r\n");
            }
        }
    }

    @TearDown(Level.Invocation)
    public void afterEach() {
        boolean ignored = file.delete();
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Measurement(timeUnit = TimeUnit.NANOSECONDS)
    public void readRfc4180RelaxedFormat(Blackhole blackhole) throws IOException, CsvException {
        try (CsvReader<List<String>> reader = CsvReader.open(file, Rfc4180RelaxedFormat.provider())) {
            while (reader.hasNext()) {
                blackhole.consume(reader.next());
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Measurement(timeUnit = TimeUnit.NANOSECONDS)
    public void readRfc4180HalfRelaxedFormat(Blackhole blackhole) throws IOException, CsvException {
        try (CsvReader<List<String>> reader = CsvReader.open(file, Rfc4180HalfRelaxedFormat.provider())) {
            while (reader.hasNext()) {
                blackhole.consume(reader.next());
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Measurement(timeUnit = TimeUnit.NANOSECONDS)
    public void readRfc4180StrictFormat(Blackhole blackhole) throws IOException, CsvException {
        try (CsvReader<List<String>> reader = CsvReader.open(file, Rfc4180StrictFormat.provider())) {
            while (reader.hasNext()) {
                blackhole.consume(reader.next());
            }
        }
    }
}
