package me.mneri.csv.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.simpleflatmapper.csv.CsvParser;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@BenchmarkMode(Mode.AverageTime)
@Fork(3)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 10, time = 5)
@Measurement(iterations = 20, time = 5)
public class SimpleFlatMapperBenchmark {
    @Benchmark
    public void worldCitiesPop(Blackhole bh) throws Exception {
        File file = new File("/home/mneri/Downloads/stop_times.txt");
        Charset charset = StandardCharsets.ISO_8859_1;

        try (Stream<String[]> stream = CsvParser.stream(new FileReader(file, charset))) {
            stream.forEach(bh::consume);
        }
    }
}
