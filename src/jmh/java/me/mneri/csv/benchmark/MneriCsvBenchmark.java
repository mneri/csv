package me.mneri.csv.benchmark;

import me.mneri.csv.CsvReader;
import me.mneri.csv.deserializer.Deserializer;
import me.mneri.csv.format.Rfc4180FullyRelaxedFormat;
import me.mneri.csv.line.RecycledLine;
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
    private static class CityDeserializer implements Deserializer<City> {
        @Override
        public City deserialize(RecycledLine line) throws IOException {
            return new City(
                    line.getString(0),
                    line.getString(1),
                    line.getString(2),
                    line.getString(3),
                    line.getString(4),
                    line.getString(5),
                    line.getString(6));
        }
    }

    @Benchmark
    public void worldCitiesPop(Blackhole bh) throws IOException {
        File file = new File("/home/mneri/Downloads/short.csv");
        Charset charset = StandardCharsets.ISO_8859_1;
        try (CsvReader<City> reader =
                     CsvReader.open(file, charset, Rfc4180FullyRelaxedFormat.provider(), new CityDeserializer())) {
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
