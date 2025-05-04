package me.mneri.csv.format;

import me.mneri.csv.exception.CsvException;
import me.mneri.csv.reader.CsvReader;
import me.mneri.csv.reader.StringListDeserializer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.Blackhole;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CsvReaderBenchmark {
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @Measurement(timeUnit = TimeUnit.NANOSECONDS)
    public void read(Blackhole blackhole) throws IOException, CsvException {
        File file = new File("/home/mneri/Downloads/worldcitiespop.csv");

        try (CsvReader<List<String>> reader = CsvReader.open(file, Rfc4180RelaxedFormat.provider(), new StringListDeserializer())) {
            while (reader.hasNext()) {
                blackhole.consume(reader.next());
            }
        }
    }
}
