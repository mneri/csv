package me.mneri.csv.benchmark;

import nbbrd.picocsv.Csv;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@Fork(3)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 6)
@Measurement(iterations = 6)
public class PicoCsvBenchmark {
    @Benchmark
    public void worldCitiesPop(Blackhole bh) throws IOException {
        File file = new File("/home/mneri/Downloads/worldcitiespop.csv");
        Charset charset = StandardCharsets.ISO_8859_1;
        try (java.io.Reader chars = new FileReader(file, charset)) {
            try (Csv.Reader reader = Csv.Reader.of(Csv.Format.DEFAULT, Csv.ReaderOptions.builder().lenientSeparator(true).build(), chars)) {
                while (reader.readLine()) {
                    String[] line = new String[7];
                    int i = 0;
                    if (!reader.isComment()) {
                        while (reader.readField()) {
                            line[i++] = reader.toString();
                        }
                    }
                    bh.consume(line);
                }
            }
        }
    }
}
