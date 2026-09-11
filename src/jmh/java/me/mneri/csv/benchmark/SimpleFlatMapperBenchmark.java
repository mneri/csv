package me.mneri.csv.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.simpleflatmapper.csv.CsvParser;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@BenchmarkMode(Mode.AverageTime)
@Fork(3)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 6)
@Measurement(iterations = 6)
public class SimpleFlatMapperBenchmark {
    @Benchmark
    public void worldCitiesPop(Blackhole bh) throws Exception {
        File file = new File("/home/mneri/Downloads/worldcitiespop.csv");
        Charset charset = StandardCharsets.ISO_8859_1;

        try (Stream<String[]> stream = CsvParser.stream(new FileReader(file, charset))) {
            stream.forEach(next -> {
                City city = new City(
                        next[0],
                        next[1],
                        next[2],
                        next[3],
                        next[4],
                        next[5],
                        next[6]);
                bh.consume(city);
            });
        }
    }
}
