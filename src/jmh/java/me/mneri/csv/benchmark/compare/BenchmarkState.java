/*
 * Copyright 2018 Massimo Neri <hello@mneri.me>
 *
 * This file is part of mneri/csv.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.mneri.csv.benchmark.compare;

import org.openjdk.jmh.annotations.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.GZIPInputStream;

@State(Scope.Benchmark)
public class BenchmarkState {
    @Param({"WORLD_CITIES_POP", "GTFS_STOP_TIMES", "GTFS_TRIPS"})
    public String datasetName;
    private BenchmarkDataset dataset;
    private Path path;

    @Setup(Level.Trial)
    public void setUp() throws IOException {
        dataset = BenchmarkDataset.valueOf(datasetName);

        Path cacheDir = cacheDir();
        path = cacheDir.resolve(dataset.name() + ".csv");
        Path checksumFile = cacheDir.resolve(dataset.name() + ".crc32");

        long sourceCrc = crc32(dataset.resourcePath());

        boolean upToDate = Files.exists(path)
                && Files.exists(checksumFile)
                && Long.parseLong(Files.readString(checksumFile).trim()) == sourceCrc;

        if (!upToDate) {
            decompress(dataset, path);
            Files.writeString(checksumFile, Long.toString(sourceCrc));
        }
    }

    public File file() {
        return path.toFile();
    }

    public Charset charset() {
        return dataset.charset();
    }

    private static Path cacheDir() throws IOException {
        Path dir = Path.of(System.getProperty("java.io.tmpdir"), "csv-benchmark-cache");
        Files.createDirectories(dir);
        return dir;
    }

    private long crc32(String resourcePath) throws IOException {
        CRC32 crc = new CRC32();
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Fixture not found on classpath: " + resourcePath);
            }
            try (CheckedInputStream checked = new CheckedInputStream(in, crc)) {
                checked.transferTo(OutputStream.nullOutputStream());
            }
        }
        return crc.getValue();
    }

    private void decompress(BenchmarkDataset dataset, Path target) throws IOException {
        Path partial = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".part");
        try (InputStream gz = getClass().getResourceAsStream(dataset.resourcePath())) {
            if (gz == null) {
                throw new IllegalStateException("Fixture not found on classpath: " + dataset.resourcePath());
            }
            try (InputStream in = new GZIPInputStream(gz);
                 OutputStream out = Files.newOutputStream(partial, StandardOpenOption.TRUNCATE_EXISTING)) {
                in.transferTo(out);
            }
        }
        Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}