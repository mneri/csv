# mneri/csv

A high-performance Java CSV parser and writer using the Java Vector API (SIMD).

`mneri/csv` is a solid, allocation-conscious CSV reader/writer for Java. The parser uses the Java Vector API
(`jdk.incubator.vector`) to accelerate delimiter detection using SIMD instructions.

See [IMPLEMENTATION.md](IMPLEMENTATION.md) for a detailed overview of the project.  

## Reading a CSV File

`CsvReader` uses a `Deserializer` to convert each CSV line into an object.

```java
try (CsvReader<Contact> reader = CsvReader.open(new File("contacts.csv"), StandardCharsets.UTF_8, new ContactDeserializer())) {
    while (reader.hasNext()) {
        Contact contact = reader.next(); // Records are mapped to domain objects via the provided ContactDeserializer
        // ...
    }
}
```

Where `ContactDeserializer` is:

```java
public class ContactDeserializer implements Deserializer<Contact> {
    @Override
    public Contact deserialize(RecycledCsvLine line) {
        Contact contact = new Contact();
        contact.setFirstName(line.getString(0));
        contact.setLastName(line.getString(1));
        // ...
        return contact;
    }
}
```

The `RecycledLine` passed to `deserialize()` is reused by the reader. Use it only inside the method. Do not store it or
return it from the method.

## Writing a CSV File

`CsvWriter` uses a `Serializer` to convert each object into a CSV line.

```java
try (CsvWriter<Contact> writer = CsvWriter.open(new File("contacts.csv"), StandardCharsets.UTF_8, new ContactSerializer())) {
    for (Contact contact : contacts) {
        writer.write(contact); // Domain objects are mapped to records via the provided ContactSerializer
    }
}
```

Where `ContactSerializer` is:

```java
public class ContactSerializer implements CsvSerializer<Contact> {
    @Override
    public void serialize(Contact person, List<String> out) {
        out.add(contact.getFirstName());
        out.add(contact.getLastName());
        // ...
    }
}
```

## Dialect Support

Dialects are called "formats". The format can be defined at the creation of a `CsvReader`.
```java
try (CsvReader<Contact> reader = CsvReader.open(new File("contacts.csv"), StandardCharsets.UTF_8, Rfc4180FullyRelaxedFormat.provider(), new ContactDeserializer())){
    while (reader.hasNext()) {
        Contact contact = reader.next();
        // ...
    }
}
```

The available formats are:

| Format                                      | Line Termination             | Variable Number of Fields[^1] | Quotes in Unqualified Fields[^2] | Extra Text After Qualified Field[^3] | Truncated Qualified Fields[^4] |
|:--------------------------------------------|:-----------------------------|:-----------------------------:|:--------------------------------:|:------------------------------------:|:------------------------------:|
| **Machintosh[^5]**                          | `\r`                         |                               |                no                |                  no                  |               no               |
| **RFC&nbsp;4180&nbsp;"Strict"**             | `\r\n`                       |                               |                no                |                  no                  |               no               |
| **RFC&nbsp;4180&nbsp;"Half&nbsp;Relaxed"**  | `\r\n`,&nbsp;`\n`            |                               |                                  |                  no                  |               no               |
| **RFC&nbsp;4180&nbsp;"Fully&nbsp;Relaxed"** | `\r\n`,&nbsp;`\r`,&nbsp;`\n` |                               |                                  |                                      |                                |
| **MS&nbsp;Excel**                           | `\r\n`,&nbsp;`\r`,&nbsp;`\n` |                               |                                  |                                      |                                |

## Vector API
`mneri/csv` features an alternative high-performance parser implementation built on top of Java's **Vector API**. By 
everaging SIMD (Single Instruction, Multiple Data) CPU instructions (such as AVX or NEON), this parser can process
chunks of data concurrently in a single CPU cycle, significantly lowering parsing time.

Because the Vector API is an incubating feature in Java (available from **Java 16 and later**), it is hidden behind an
incubator module. The Vector API can be enabled via the JVM flag `--add-modules jdk.incubator.vector`.

## Performances

See [PERFORMANCE.md](PERFORMANCE.md) for the full results, performance analysis, hardware details, and commands for running
the benchmarks.

Below, the comparison of `mneri/csv` performances against other Java frameworks using the popular `worldcitiespop.csv`
benchmark. `mneri/csv` in Parallel/Vector configuration currently score the fastest.

| Dataset              | Rank | Benchmark                             | Score (ms/op) |    Error |
|----------------------|-----:|---------------------------------------|--------------:|---------:|
| **WORLD_CITIES_POP** | 🥇 1 | `mneri/csv` (Parallel/Vector)         |   **269.871** |  ± 4.864 |
|                      | 🥈 2 | `sesseltjonna-csv`                    |   **301.261** |  ± 2.603 |
|                      | 🥉 3 | `mneri/csv` (Parallel/Scalar)         |   **355.175** | ± 42.196 |
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

[^1]: **Variable Number of Fields**: the format accepts files containing a different number of fields on
different lines.
[^2]: **Quotes in Unqualified Fields**: the format accepts unqualified fields containing double quotes (`"`); for
example, the line `aaa,b"b"b,ccc CRLF` is interpreted as ⟨`aaa`, `b"b"b`, `ccc`⟩.
[^3]: **Extra Text After Qualified Field**: the format accepts free text after the closing double quotes (`"`)
of a qualified field; for example, the line `aaa,"bb"b,ccc` is interpreted as ⟨`aaa`, `bbb`, `ccc`⟩.
[^4]: **Truncated Qualified Fields**: the format accepts a field starting with a double quote character (`"`)
but the end of file is reached prior to the corresponding closing double quote; for example, the line
`aaa,bbb,"ccc EOF` is interpreted as ⟨`aaa`, `bbb`, `ccc`⟩.
[^5]: **Macintosh Format**: refers to the legacy line-termination convention (`\r`) used by classic Mac OS systems
prior to the transition to Unix-based OS X in 2001.
