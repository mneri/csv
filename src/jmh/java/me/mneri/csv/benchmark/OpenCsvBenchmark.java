package me.mneri.csv.benchmark;

import com.opencsv.CSVReader;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@Fork(3)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 10, time = 5)
@Measurement(iterations = 20, time = 5)
public class OpenCsvBenchmark {
//    @Benchmark
//    public void worldCitiesPop(Blackhole bh) throws Exception {
//        File file = new File("/home/mneri/Downloads/stop_times.txt");
//        Charset charset = StandardCharsets.ISO_8859_1;
//
//        try (CSVReader reader = new CSVReader(new FileReader(file, charset))) {
//            String[] next;
//            while ((next = reader.readNext()) != null) {
//                bh.consume(next);
//            }
//        }
//    }
}
