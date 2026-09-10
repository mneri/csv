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
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 6)
@Measurement(iterations = 6)
public abstract class MneriCsvBenchmark {
    @Benchmark
    public void worldCitiesPop(Blackhole bh) throws IOException {
        File file = new File("/home/mneri/Downloads/worldcitiespop.csv");
        Charset charset = StandardCharsets.ISO_8859_1;
        try (CsvReader<String[]> reader = CsvReader.open(file, charset, new StringArrayDeserializer())) {
            while (reader.hasNext()) {
                bh.consume(reader.next());
            }
        }
    }

    @Fork(3)
    public static class Sequential extends MneriCsvBenchmark {
    }

    @Fork(value = 3, jvmArgsPrepend = "--add-modules=jdk.incubator.vector")
    public static class Vector extends MneriCsvBenchmark {
    }
}
