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

package me.mneri.csv.benchmark;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public enum BenchmarkDataset {
    WORLD_CITIES_POP("/data/maxmind/worldcitiespop.csv.gz", StandardCharsets.ISO_8859_1),
    GTFS_STOP_TIMES("/data/gtfs/stop_times.txt.gz", StandardCharsets.UTF_8),
    GTFS_TRIPS("/data/gtfs/trips.txt.gz", StandardCharsets.UTF_8);

    private final String resourcePath;
    private final Charset charset;

    BenchmarkDataset(String resourcePath, Charset charset) {
        this.resourcePath = resourcePath;
        this.charset = charset;
    }

    public String resourcePath() {
        return resourcePath;
    }

    public Charset charset() {
        return charset;
    }
}
