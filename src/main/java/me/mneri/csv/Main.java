package me.mneri.csv;

import me.mneri.csv.exception.CsvException;
import me.mneri.csv.format.Rfc4180RelaxedFormat;
import me.mneri.csv.reader.CsvReader;
import me.mneri.csv.reader.Deserializer;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final int WARMUP_ROUNDS = 40;
    private static final int ROUNDS = 40;

    private final Deserializer<Integer> deserializer = line -> {
        int hashCode = 0;
        int count = line.getFieldCount();
        for (int i = 0; i < count; i++) {
            String s = line.getString(i);
            hashCode ^= s != null ? s.hashCode() : 0;
        }
        return hashCode;
    };

    public static void main(String... args) throws IOException, CsvException {
        Main main = new Main();

        long warmupTimeNanos = 0;
        for (int i = 0; i < WARMUP_ROUNDS; i++) {
            warmupTimeNanos += main.execute();
        }
        System.out.println(TimeUnit.NANOSECONDS.toMillis(warmupTimeNanos / WARMUP_ROUNDS));

        long timeNanos = 0;
        for (int i = 0; i < ROUNDS; i++) {
            timeNanos += main.execute();
        }
        System.out.println(TimeUnit.NANOSECONDS.toMillis(timeNanos / ROUNDS));
    }

    private long execute() throws IOException, CsvException {
        File file = new File("/home/mneri/Downloads/worldcitiespop.csv");

        try (CsvReader<Integer> reader = CsvReader.open(file, Rfc4180RelaxedFormat.provider(), deserializer)) {
            int blackhole = 0;
            long startTimeNanos = System.nanoTime();
            while (reader.hasNext()) {
                blackhole ^= reader.next();
            }
            long timeNanos = System.nanoTime() - startTimeNanos;
            System.out.println(blackhole + " [" + TimeUnit.NANOSECONDS.toMillis(timeNanos) + "]");
            return timeNanos;
        }
    }
}
