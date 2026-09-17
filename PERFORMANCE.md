# Benchmark Results
| Dataset              | Rank | Benchmark                | Score (ms/op) |    Error |
|----------------------|-----:|--------------------------|--------------:|---------:|
| **WORLD_CITIES_POP** |    1 | `sesseltjonna-csv`       |   **302.635** |  ± 1.895 |
|                      |    2 | `mneri/csv` (Vector API) |   **474.905** | ± 14.964 |
|                      |    3 | `SimpleFlatMapper`       |   **502.712** |  ± 8.108 |
|                      |    4 | `FastCSV`                |   **505.844** |  ± 6.906 |
|                      |    5 | `univocity-parsers`      |   **537.645** |  ± 8.434 |
|                      |    6 | `mneri/csv` (Sequential) |   **598.214** |  ± 5.530 |
|                      |    7 | `opencsv`                | **1,198.996** | ± 14.248 |
|                      |    8 | `Apache Commons CSV`     | **2,723.402** | ± 13.448 |
| **GTFS_STOP_TIMES**  |    1 | `sesseltjonna-csv`       |   **448.088** |  ± 3.640 |
|                      |    2 | `SimpleFlatMapper`       |   **666.533** |  ± 7.048 |
|                      |    3 | `mneri/csv` (Vector API) |   **674.093** |  ± 7.697 |
|                      |    4 | `FastCSV`                |   **728.479** |  ± 8.795 |
|                      |    5 | `univocity-parsers`      |   **733.639** | ± 10.656 |
|                      |    6 | `mneri/csv` (Sequential) |   **882.787** | ± 10.441 |
|                      |    7 | `opencsv`                | **1,545.418** | ± 20.339 |
|                      |    8 | `Apache Commons CSV`     | **5,224.941** | ± 29.130 |
| **GTFS_TRIPS**       |    1 | `sesseltjonna-csv`       |    **24.174** |  ± 0.274 |
|                      |    2 | `SimpleFlatMapper`       |    **25.591** |  ± 0.518 |
|                      |    3 | `mneri/csv` (Vector API) |    **27.791** |  ± 1.059 |
|                      |    4 | `univocity-parsers`      |    **27.839** |  ± 0.421 |
|                      |    5 | `FastCSV`                |    **33.321** |  ± 0.445 |
|                      |    6 | `mneri/csv` (Sequential) |    **40.957** |  ± 0.494 |
|                      |    7 | `opencsv`                |    **69.657** |  ± 0.966 |
|                      |    8 | `Apache Commons CSV`     |   **255.345** | ± 13.122 |

# Hardware Specifications
| Category                | Specification                                          |
|-------------------------|--------------------------------------------------------|
| **Laptop**              | Dell XPS 13 7390                                       |
| **CPU**                 | Intel Core i7-10510U                                   |
| **CPU Cores / Threads** | 4 cores / 8 threads                                    |
| **CPU Max Frequency**   | 4.9 GHz                                                |
| **GPU**                 | Intel UHD Graphics (Comet Lake-U GT2)                  |
| **RAM**                 | 16 GB LPDDR3-2133                                      |
| **Storage**             | Toshiba KXG60ZNV512G NVMe SSD                          |
| **Storage Capacity**    | 512.11 GB                                              |
| **Storage Firmware**    | `10604107`                                             |
| **Operating System**    | Ubuntu 26.04.1 LTS                                     |
| **Kernel**              | Linux 7.0.0-31-generic                                 |
| **Architecture**        | x86-64                                                 |
| **SIMD Support**        | SSE, SSE2, SSE3, SSSE3, SSE4.1, SSE4.2, AVX, AVX2, FMA |
| **Maximum SIMD Width**  | 256-bit (AVX2)                                         |
| **Power Profile**       | Balanced                                               |
| **Discrete GPU**        | None                                                   |

# Running the benchmarks

The performance tests are implemented using [JMH (Java Microbenchmark Harness)](https://github.com/openjdk/jmh).

The benchmarks should be run with **JDK 21**. If you need to change your Java version or verify which Java version Gradle is using, see [Checking the Java and Gradle versions](#checking-the-java-and-gradle-versions) at the bottom of this document.

## Running all benchmarks

From the repository root:

```bash
./gradlew jmh
```

The benchmark results are generated under:

```text
build/results/jmh/
```

## Running an individual benchmark

The benchmarks can also be run using the standalone JMH JAR.

First build it:

```bash
./gradlew jmhJar
```

List the available benchmarks:

```bash
java -jar build/libs/*-jmh.jar -l
```

Run the sequential implementation:

```bash
java -jar build/libs/*-jmh.jar '.*MneriCsvBenchmark.Sequential.*'
```

Run the Vector implementation:

```bash
java -jar build/libs/*-jmh.jar '.*MneriCsvBenchmark.Vector.*'
```

## Running a specific dataset

The benchmarks use the `datasetName` parameter. Available datasets include:

- `WORLD_CITIES_POP`
- `GTFS_STOP_TIMES`
- `GTFS_TRIPS`

For example, to run the sequential implementation against `WORLD_CITIES_POP`:

```bash
java -jar build/libs/*-jmh.jar \
  '.*MneriCsvBenchmark.Sequential.*' \
  -p datasetName=WORLD_CITIES_POP
```

## Quick benchmark run

For a quick test, use fewer warm-up and measurement iterations:

```bash
java -jar build/libs/*-jmh.jar \
  '.*MneriCsvBenchmark.Sequential.*' \
  -wi 2 \
  -i 3 \
  -f 1
```

These settings are intended for a quick sanity check and should not be used to compare results with the published benchmark results.

## Reproducing the published benchmark configuration

The published results were generated with the following JMH configuration:

- 6 warm-up iterations
- 10 seconds per warm-up iteration
- 6 measurement iterations
- 10 seconds per measurement iteration
- 3 forks
- 1 thread

For example:

```bash
java -jar build/libs/*-jmh.jar \
  '.*MneriCsvBenchmark.Sequential.*' \
  -wi 6 \
  -w 10s \
  -i 6 \
  -r 10s \
  -f 3 \
  -t 1
```

Add the dataset parameter to run a specific dataset:

```bash
java -jar build/libs/*-jmh.jar \
  '.*MneriCsvBenchmark.Sequential.*' \
  -p datasetName=WORLD_CITIES_POP \
  -wi 6 \
  -w 10s \
  -i 6 \
  -r 10s \
  -f 3 \
  -t 1
```

Replace `WORLD_CITIES_POP` with `GTFS_STOP_TIMES` or `GTFS_TRIPS` as required.

> **Note:** Benchmark results depend on the JDK version, CPU, operating system, JVM configuration, and system load. For meaningful comparisons with the published results, use the same JDK and benchmark configuration.

## Checking the Java and Gradle versions

The benchmarks should be run with **JDK 21**.

Check the installed Java version:

```bash
java -version
```

Check which Java version Gradle is using:

```bash
./gradlew --version
```

If multiple JDK versions are installed on Debian/Ubuntu, select JDK 21 with:

```bash
sudo update-alternatives --config java
sudo update-alternatives --config javac
```

Alternatively, set `JAVA_HOME` explicitly:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
```

Then verify:

```bash
java -version
./gradlew --version
```

The Gradle output should show JDK 21 as the JVM being used.
