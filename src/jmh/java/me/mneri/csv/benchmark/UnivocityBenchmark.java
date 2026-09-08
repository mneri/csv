package me.mneri.csv.benchmark;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@Fork(3)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 10, time = 5)
@Measurement(iterations = 20, time = 5)
public class UnivocityBenchmark {
    @Benchmark
    public void worldCitiesPop(Blackhole bh) throws Exception {
        File file = new File("/home/mneri/Downloads/stop_times.txt");
        Charset charset = StandardCharsets.ISO_8859_1;

        CsvParserSettings settings = new CsvParserSettings();
        CsvParser parser = new CsvParser(settings);
        List<String[]> allRows = parser.parseAll(new FileReader(file, charset));
        for (String[] row : allRows) {
            bh.consume(row);
        }
    }
}
