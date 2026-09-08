package me.mneri.csv.benchmark;

import me.mneri.csv.CsvReader;
import me.mneri.csv.deserializer.StringArrayDeserializer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@Fork(3)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 10, time = 5)
@Measurement(iterations = 20, time = 5)
public class MneriCsvBenchmark {
    @Benchmark
    public void worldCitiesPop(Blackhole bh) throws IOException {
        File file = new File("/home/mneri/Downloads/stop_times.txt");
        Charset charset = StandardCharsets.ISO_8859_1;
        try (CsvReader<String[]> reader = CsvReader.open(file, charset, new StringArrayDeserializer())) {
            while (reader.hasNext()) {
                bh.consume(reader.next());
            }
        }
    }
}
