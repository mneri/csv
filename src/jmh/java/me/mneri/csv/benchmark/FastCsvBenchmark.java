package me.mneri.csv.benchmark;

import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.CsvRecord;
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
@Warmup(iterations = 10, time = 5)
@Measurement(iterations = 20, time = 5)
public class FastCsvBenchmark {
//    @Benchmark
//    public void worldCitiesPop(Blackhole bh) throws IOException {
//        File file = new File("/home/mneri/Downloads/stop_times.txt");
//        Charset charset = StandardCharsets.ISO_8859_1;
//        try (CsvReader<CsvRecord> csv = CsvReader.builder().ofCsvRecord(new FileReader(file, charset))) {
//            csv.forEach(bh::consume);
//        }
//    }
}
