package me.mneri.csv.format;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Thread)
public class FormatBenchmark {
    private static final int SIZE = 1 << 16;

    /**
     * Fraction of characters that are one of the four special characters (',', '\r', '\n', '"') rather than an
     * ordinary character. 0.4141 reproduces the exact ratio cited in the source comment above; the others let you
     * see how the fast path's value changes as CSVs get denser with special characters.
     */
    @Param({"0.0", "0.05", "0.15", "0.4141", "0.75", "1.0"})
    public double specialRatio;

    private Format format;
    private char[] input;

    @Setup(Level.Trial)
    public void beforeTrial() {
        format = Rfc4180FullyRelaxedFormat.provider().provide();

        // Fixed seed: every JMH fork/iteration at a given specialRatio sees the identical character sequence, so
        // differences across runs are attributable to the code under test, not to input variance.
        Random rnd = new Random(42);
        char[] specials = {',', '\r', '\n', '"'};
        input = new char[SIZE];
        for (int i = 0; i < SIZE; i++) {
            input[i] = (rnd.nextDouble() < specialRatio) ? specials[rnd.nextInt(specials.length)] : 'a';
        }
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public void consume(Blackhole bh) {
        int s = format.base();
        for (char c : input) {
            s = format.consume(s, c);
        }
        bh.consume(s);
    }

    @Benchmark
    @OperationsPerInvocation(SIZE)
    public void consumeSlow(Blackhole bh) {
        int s = format.base();
        for (char c : input) {
            s = format.consumeSlow(s, c);
        }
        bh.consume(s);
    }
}
