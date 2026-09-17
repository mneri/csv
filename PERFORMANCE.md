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

