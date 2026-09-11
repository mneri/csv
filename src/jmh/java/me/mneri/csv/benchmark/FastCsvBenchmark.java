package me.mneri.csv.benchmark;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
import de.siegmar.fastcsv.reader.FieldMismatchStrategy;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@Fork(3)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 6)
@Measurement(iterations = 6)
public class FastCsvBenchmark {
    @Benchmark
    public void worldCitiesPop(Blackhole bh) throws IOException {
        File file = new File("/home/mneri/Downloads/short.csv");
        Charset charset = StandardCharsets.ISO_8859_1;
        try (CsvReader<CsvRecord> csv = CsvReader.builder()
                .extraFieldStrategy(FieldMismatchStrategy.IGNORE)
                .missingFieldStrategy(FieldMismatchStrategy.IGNORE)
                .ofCsvRecord(new FileReader(file, charset))) {
            csv.forEach(r -> {
                City city = new City(
                        r.getField(0),
                        r.getField(1),
                        r.getField(2),
                        r.getField(3),
                        r.getField(4),
                        r.getField(5),
                        r.getField(6));
                bh.consume(city);
            });
        }
    }
}
