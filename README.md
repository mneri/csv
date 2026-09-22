# mneri/csv

`mneri/csv` is a fast and easy-to-use library for reading and writing CSV files.

## Reading a CSV File

`CsvReader` uses a `Deserializer` to convert each CSV line into an object.

```java
try(CsvReader<Contact> reader = CsvReader.open(new File("contacts.csv"), StandardCharsets.UTF_8, new ContactDeserializer())){
    while(reader.hasNext()) {
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
try(CsvWriter<Contact> writer = CsvWriter.open(new File("contacts.csv"), StandardCharsets.UTF_8, new ContactSerializer())){
    for(Contact contact : contacts) {
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

| Format                                      | Line Termination             | Variable Number of Fields[^1] | Quotes in Unqualified Fields[^2] | Extra Text After Quoted Field[^3] | Truncated Quoted Fields[^4] |
|:--------------------------------------------|:-----------------------------|:-----------------------------:|:--------------------------------:|:---------------------------------:|:---------------------------:|
| **Machintosh[^5]**                          | `\r`                         |                               |                no                |                no                 |             no              |
| **RFC&nbsp;4180&nbsp;"Strict"**             | `\r\n`                       |                               |                no                |                no                 |             no              |
| **RFC&nbsp;4180&nbsp;"Half&nbsp;Relaxed"**  | `\r\n`,&nbsp;`\n`            |                               |                                  |                no                 |             no              |
| **RFC&nbsp;4180&nbsp;"Fully&nbsp;Relaxed"** | `\r\n`,&nbsp;`\r`,&nbsp;`\n` |                               |                                  |                                   |                             |
| **MS&nbsp;Excel**                           | `\r\n`,&nbsp;`\r`,&nbsp;`\n` |                               |                                  |                                   |                             |

## Vector API
`mneri/csv` features an alternative high-performance parser implementation built on top of Java's **Vector API**. By 
everaging SIMD (Single Instruction, Multiple Data) CPU instructions (such as AVX or NEON), this parser can process
chunks of data concurrently in a single CPU cycle, significantly lowering parsing time.

Because the Vector API is an incubating feature in Java (available from **Java 16 and later**), it is hidden behind an
incubator module. The Vector API can be enabled via the JVM flag `--add-modules jdk.incubator.vector`.

## Performances

The project is designed for large CSV files and low overhead. It uses:

* Transition tables instead of a large chain of `if`-`else` branches.
* Recycled line buffers.
* Sequential and Vector API parsers.
* Small hot methods and separate slow paths for errors and unusual calls.

The published benchmarks compare `mneri/csv` with several Java CSV libraries. Results depend on the dataset, JDK, CPU,
operating system, JVM options, and system load. See [PERFORMANCE.md](PERFORMANCE.md) for the full results, hardware
details, and commands for running the benchmarks.

Below, the comparison of `mneri/csv` performances against other Java frameworks using the popular `worldcitiespop.txt`
benchmark.

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

For more benchmark results, see [PERFORMANCE.md](PERFORMANCE.md).

[^1]: **Variable Number of Fields**: the format accepts files containing a different number of fields on
different lines.
[^2]: **Quotes in Unqualified Fields**: the format accepts unqualified fields containing double quotes (`"`); for
example, the line `aaa,b"b"b,ccc CRLF` is interpreted as ⟨`aaa`, `b"b"b`, `ccc`⟩.
[^3]: **Extra Text After Quoted Field**: the format accepts free text after the closing double quotes (`"`)
of a qualified field; for example, the line `aaa,"bb"b,ccc` is interpreted as ⟨`aaa`, `bbb`, `ccc`⟩.
[^4]: **Truncated Quoted Fields**: the format accepts a field starting with a double quote character (`"`)
but the end of file is reached prior to the corresponding closing double quote; for example, the line
`aaa,bbb,"ccc EOF` is interpreted as ⟨`aaa`, `bbb`, `ccc`⟩.
[^5]: **Macintosh Format**: refers to the legacy line-termination convention (`\r`) used by classic Mac OS systems
prior to the transition to Unix-based OS X in 2001.