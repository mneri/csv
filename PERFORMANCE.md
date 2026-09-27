# Performance Comparison with Popular Parsers

See section _[Running the Benchmarks](#running-the-benchmarks)_ to understand how to replicate these results, and
_[Hardware Specifications](#hardware-specifications)_ for details about the environment. See
[PERFORMANCE.jmh](https://github.com/mneri/csv/blob/master/PERFORMANCE.jmh) document for the full, unredacted output of
the benchmark harness.

The benchmark measures the execution time in milliseconds across three distinct datasets, shown in the table below.

| Dataset              |     Size |     Lines | Fields per Line | Avg. Line Length | Quoted Fields |
|----------------------|---------:|----------:|----------------:|-----------------:|---------------|
| **WORLD_CITIES_POP** | 129.2 MB | 2,699,354 |               7 |         48 chars | Rare          |
| **GTFS_STOP_TIMES**  | 253.1 MB | 3,116,850 |               9 |         81 chars | None          |
| **GTFS_TRIPS**       |  12.3 MB |   114,393 |             ~ 8 |        107 chars | None          |

`mneri/csv` appears under four configurations along two independent axes: _vectorization_ and _parallelism._

* **Vectorization (Scalar vs. Vector)**: The _Vector API_ was introduced in Java 16 and, if supported by the
  environment, the parser leverages the CPU's SIMD instruction set to process a CSV file in 64-character blocks. If not,
  the parser falls back to processing the stream character by character. The Vector API is still in incubation and must
  be explicitly enabled via the JVM option `--add-modules=jdk.incubator.vector`.
* **Parallelism (Sequential vs. Parallel)**: `mneri/csv` exposes two separate APIs: `CsvReader.open()` and
  `CsvReader.parallel()`. In parallel mode, a background thread reads the input and parses the fields, while the object
  deserialization is left to the client's thread (i.e., instantiating the domain objects).

| Dataset              | Rank | Benchmark                             | Score (ms/op) |    Error |
|----------------------|-----:|---------------------------------------|--------------:|---------:|
| **WORLD_CITIES_POP** |    1 | `mneri/csv` (Parallel/Vector)         |   **269.871** |  ± 4.864 |
|                      |    2 | `sesseltjonna-csv`                    |   **301.261** |  ± 2.603 |
|                      |    3 | `mneri/csv` (Parallel/Scalar)         |   **355.175** | ± 42.196 |
|                      |    4 | `mneri/csv` (Sequential/Vector)       |   **386.606** |  ± 1.966 |
|                      |    5 | `SimpleFlatMapper`                    |   **490.333** |  ± 4.741 |
|                      |    6 | `univocity-parsers` (Parallel Reader) |   **496.941** |  ± 5.912 |
|                      |    7 | `FastCSV`                             |   **504.354** |  ± 4.661 |
|                      |    8 | `univocity-parsers` (Standard Reader) |   **521.132** |  ± 7.527 |
|                      |    9 | `mneri/csv` (Sequential/Scalar)       |   **532.251** | ± 26.342 |
|                      |   10 | `Quick CSV Streamer`                  |   **534.030** |  ± 5.720 |
|                      |   11 | `picocsv`                             |   **551.995** |  ± 5.128 |
|                      |   12 | `Jackson CSV`                         |   **774.857** |  ± 3.951 |
|                      |   13 | `JavaCSV`                             | **1,066.995** | ± 22.780 |
|                      |   14 | `Super CSV`                           | **1,176.623** | ± 13.487 |
|                      |   15 | `opencsv`                             | **1,181.589** |  ± 6.937 |
|                      |   16 | `Apache Commons CSV`                  | **2,720.958** | ± 10.256 |
| **GTFS_STOP_TIMES**  |    1 | `mneri/csv` (Parallel/Vector)         |   **360.476** |  ± 2.952 |
|                      |    2 | `sesseltjonna-csv`                    |   **445.516** |  ± 3.776 |
|                      |    3 | `mneri/csv` (Parallel/Scalar)         |   **516.884** | ± 16.110 |
|                      |    4 | `mneri/csv` (Sequential/Vector)       |   **544.439** | ± 13.690 |
|                      |    5 | `SimpleFlatMapper`                    |   **664.271** |  ± 7.708 |
|                      |    6 | `univocity-parsers` (Parallel Reader) |   **693.279** |  ± 8.635 |
|                      |    7 | `picocsv`                             |   **711.660** |  ± 5.990 |
|                      |    8 | `mneri/csv` (Sequential/Scalar)       |   **715.322** | ± 64.274 |
|                      |    9 | `univocity-parsers` (Standard Reader) |   **717.283** | ± 36.986 |
|                      |   10 | `FastCSV`                             |   **724.135** |  ± 7.147 |
|                      |   11 | `Quick CSV Streamer`                  |   **758.510** |  ± 5.187 |
|                      |   12 | `Jackson CSV`                         | **1,045.439** | ± 15.308 |
|                      |   13 | `opencsv`                             | **1,507.987** | ± 14.178 |
|                      |   14 | `JavaCSV`                             | **1,741.311** | ± 66.842 |
|                      |   15 | `Super CSV`                           | **1,842.576** | ± 41.252 |
|                      |   16 | `Apache Commons CSV`                  | **5,214.185** | ± 22.720 |
| **GTFS_TRIPS**       |    1 | `mneri/csv` (Parallel/Vector)         |    **20.889** |  ± 0.081 |
|                      |    2 | `sesseltjonna-csv`                    |    **24.081** |  ± 0.231 |
|                      |    3 | `mneri/csv` (Sequential/Vector)       |    **26.401** |  ± 1.091 |
|                      |    4 | `univocity-parsers` (Parallel Reader) |    **26.413** |  ± 0.264 |
|                      |    5 | `SimpleFlatMapper`                    |    **28.248** |  ± 2.005 |
|                      |    6 | `Quick CSV Streamer`                  |    **28.334** |  ± 0.282 |
|                      |    7 | `mneri/csv` (Parallel/Scalar)         |    **29.290** |  ± 0.130 |
|                      |    8 | `FastCSV`                             |    **33.140** |  ± 0.184 |
|                      |    9 | `univocity-parsers` (Standard Reader) |    **33.196** |  ± 0.768 |
|                      |   10 | `picocsv`                             |    **33.502** |  ± 0.245 |
|                      |   11 | `mneri/csv` (Sequential/Scalar)       |    **38.963** |  ± 0.231 |
|                      |   12 | `Jackson CSV`                         |    **41.487** |  ± 0.502 |
|                      |   13 | `opencsv`                             |    **68.600** |  ± 0.460 |
|                      |   14 | `JavaCSV`                             |    **78.903** |  ± 2.800 |
|                      |   15 | `Super CSV`                           |    **84.778** |  ± 1.195 |
|                      |   16 | `Apache Commons CSV`                  |   **244.740** |  ± 3.017 |

> **Note**: `univocity-parsers` includes a parallel reader (which is enabled by default), and both execution times are
> reported in the table.

> **Note**: `SimpleFlatMapper` includes a `ParallelReader`, which is _excluded_ from the results. In this case, the
> warm-up rounds of the benchmark cancelled the benefit of a `ParallelReader`: at the beginning of the measurement
> iterations the file is cached in the OS's page cache and once disk I/O latency is eliminated, the thread
> synchronization and queue contention overhead in `ParallelReader` outweigh its benefits (i.e., the executions with
> `ParallelReader` were measurably slower than the ones without).

`mneri/csv` ranks fastest in all three datasets when configured in Parallel/Vector mode. The Vector configurations
require Java 16+ with the JVM option `--add-modules=jdk.incubator.vector`; the Sequential/Scalar and Parallel/Scalar
configurations are available on every supported Java version.

## Performance Breakdown

Starting from the Sequential/Vector configuration (single-threaded, reading characters in blocks of 64), we isolate the
execution times. The table below measures read, parse and deserialize times on the `WORLD_CITIES_POP` dataset.

| Dataset              | Phase                                  | Class                     | Execution Time | Execution % |
|----------------------|----------------------------------------|---------------------------|----------------|-------------|
| **WORLD_CITIES_POP** | Reading (and decoding)                 | `java.io.Reader`          | 26.2 ms        | 6.7%        |
|                      | Parsing                                | `VectorLineParser`        | 159.6 ms       | 40.5%       |
|                      | Deserializing (domain object creation) | `StringArrayDeserializer` | 207.9 ms       | 52.8%       |
|                      | **Total**                              |                           | **393.7 ms**   | **100%**    |

> **Note**: It is not possible to measure these parts in isolation with JMH; the values are obtained by measuring
> cumulative stages (read; read and parse; read, parse and deserialize) and subtracting. The phases compete for the same
> caches and branch predictors, so the split is approximate.

In the benchmark, I/O is not a bottleneck. The file is being read from the OS page cache, and the UTF-8 decoder is
taking its fast path for plain ASCII. This would be different with no warmup iterations.

_Deserialization takes more than half of the total execution time_, but it's mostly out of the hands of the parser.
The parser doesn't allocate new objects, but only leaves "pointers" behind. The `StringArrayDeserializer` picks up those
"pointers" and creates domain objects (in the benchmark, `String[]`s containing `String`s for the seven fields of the
CSV line). Memory allocations are cheap, but not free: each `new String()` scans the characters to check they fit in
Latin-1, allocates a `byte[]`, and copies the data; plus the cost of allocation of the `String[]` and the `String`
objects themselves.

In the Parallel/Vector configuration (multithreaded, processing characters in blocks of 64) parsing and deserialization
happen concurrently.

| Phase                                  | Thread                 |
|----------------------------------------|------------------------|
| Reading (and decoding)                 | T1 (Background Thread) |
| Parsing                                | T1 (Background Thread) |
| Deserializing (domain object creation) | T2 (Client Thread)     |

In `WORLD_CITIES_POP` _the cost of reading and parsing is dominated by the cost of deserialization, and absorbed._

As shown in section [Performance Comparison with Popular Parsers](#performance-comparison-with-popular-parsers), the
Parallel/Vector configuration processes the `WORLD_CITIES_POP` dataset in an average of `269.871` milliseconds. With
deserialization setting a critical-path floor of `207.9 ms`, the remaining `~62 ms` can be attributed to:

* Thread synchronization via `ArrayBlockingQueue`.
* Cache lines crossing from one core's cache to the other's.
* Hyper-Threading occasionally scheduling two threads on the same core, making them compete for the same CPU execution
  units.

The parallel reader needs a free physical core to pay off. On a busy machine, or when the two threads share a core, it
can be significantly slower. Its ceiling is the cost of deserialization, which always runs on the client's thread: the
cheaper the domain objects, the larger the gain.

## Memory Allocation and Garbage Collection

The table below shows the memory allocated in one pass over the `WORLD_CITIES_POP` dataset, measured with the JMH GC
profiler (`-prof gc`) in the Sequential/Vector configuration.

| Phase                              | Allocated per Pass | Allocation Rate | Total GC Count | Total GC Time |
|------------------------------------|-------------------:|----------------:|---------------:|--------------:|
| Reading                            |            16.9 KB |      0.6 MB/sec |             ~0 |            ~0 |
| Reading and Parsing                |             3.6 MB |     18.8 MB/sec |              8 |         14 ms |
| Reading, Parsing and Deserializing |           971.1 MB |  2,247.6 MB/sec |            558 |        286 ms |

> **Note**: _GC Count_ and _GC Time_ are totals over all the measurement iterations (6 iterations of 10 seconds), not
> per pass.

The parser allocates no objects per line or per field. The `3.6 MB` measured in the parsing phase are allocated by
`java.io.InputStreamReader`, not by the parser: about `110 bytes` for each `read()` call.

Deserializing `WORLD_CITIES_POP` into `String[]` allocates about `967 MB` per pass, 7.5 times the size of the input:

| Object                          | Size per Line |
|---------------------------------|--------------:|
| `String[]` (seven elements)     |          48 B |
| `String` objects and their data |        ~310 B |
| **Total**                       |    **~358 B** |

Nonetheless, garbage collection takes about `0.5%` of the execution time. Every object is short-lived: it is created,
handed to the benchmark's blackhole, and discarded before the next collection, and a young-generation collection costs
time in proportion to the objects that _survive_ it, not to the ones that die. The cost of deserialization lies in
allocating and filling about a gigabyte of objects (a `new String()` scans the characters, allocates a `byte[]` and
copies them), not in collecting them.

The command used to measure the allocations is the following

```bash
java -jar build/libs/*-jmh.jar 'StagesBenchmark' -p datasetName=WORLD_CITIES_POP -prof gc -wi 6 -w 10s -i 6 -r 10s -f 3 -t 1
```

# Hardware Specifications

| Category                | Specification                                          |
|-------------------------|--------------------------------------------------------|
| **Laptop**              | Dell XPS 13 7390                                       |
| **CPU**                 | Intel Core i7-10510U                                   |
| **CPU Cores / Threads** | 4 cores / 8 threads                                    |
| **CPU Max Frequency**   | 4.9 GHz                                                |
| **Architecture**        | x86-64                                                 |
| **L1 Cache**            | 32 KB data, 32 KB instruction per core                 |
| **L2 Cache**            | 256 KB per core                                        |
| **L3 Cache**            | 8 MB shared                                            |
| **Cache Line**          | 64 bytes                                               |
| **SIMD Support**        | SSE, SSE2, SSE3, SSSE3, SSE4.1, SSE4.2, AVX, AVX2, FMA |
| **Maximum SIMD Width**  | 256-bit (AVX2)                                         |
| **GPU**                 | Intel UHD Graphics (Comet Lake-U GT2)                  |
| **RAM**                 | 16 GB LPDDR3-2133                                      |
| **Storage**             | Toshiba KXG60ZNV512G NVMe SSD                          |
| **Storage Capacity**    | 512.11 GB                                              |
| **Storage Firmware**    | `10604107`                                             |
| **Operating System**    | Ubuntu 26.04.1 LTS                                     |
| **Kernel**              | Linux 7.0.0-31-generic                                 |
| **Power Profile**       | Balanced                                               |
| **Discrete GPU**        | None                                                   |
| **Java version**        | `openjdk` version `21.0.12.1` 2026-08-18               |

# Running the Benchmarks

The performance tests are implemented using [JMH (Java Microbenchmark Harness)](https://github.com/openjdk/jmh).

The benchmarks should be run with **JDK 21**. If you need to change your Java version or verify which Java version
Gradle is using, see [Checking the Java and Gradle versions](#checking-the-java-and-gradle-versions) at the bottom of
this document.

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

Run the sequential implementations:

```bash
java -jar build/libs/*-jmh.jar '.*MneriCsvBenchmark.Sequential.*'
```

Run the parallel implementations:

```bash
java -jar build/libs/*-jmh.jar '.*MneriCsvBenchmark.Parallel.*'
```

## Running a specific dataset

The benchmarks use the `datasetName` parameter. Available datasets include:

- `WORLD_CITIES_POP`
- `GTFS_STOP_TIMES`
- `GTFS_TRIPS`

For example, to run the sequential implementations against `WORLD_CITIES_POP`:

```bash
java -jar build/libs/*-jmh.jar '.*MneriCsvBenchmark.Sequential.*' -p datasetName=WORLD_CITIES_POP
```

## Quick benchmark run

For a quick test, use fewer warm-up and measurement iterations:

```bash
java -jar build/libs/*-jmh.jar '.*MneriCsvBenchmark.Sequential.*' -wi 2 -i 3 -f 1
```

These settings are intended for a quick sanity check and should not be used to compare results with the published
benchmark results.

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
java -jar build/libs/*-jmh.jar '.*MneriCsvBenchmark.Sequential.*' -wi 6 -w 10s -i 6 -r 10s -f 3 -t 1
```

Add the dataset parameter to run a specific dataset:

```bash
java -jar build/libs/*-jmh.jar '.*MneriCsvBenchmark.Sequential.*' -p datasetName=WORLD_CITIES_POP -wi 6 -w 10s -i 6 -r 10s -f 3 -t 1
```

Replace `WORLD_CITIES_POP` with `GTFS_STOP_TIMES` or `GTFS_TRIPS` as required.

> **Note:** Benchmark results depend on the JDK version, CPU, operating system, JVM configuration, and system load. For
> meaningful comparisons with the published results, use the same JDK and benchmark configuration.

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
