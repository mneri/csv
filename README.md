# mneri/csv

`mneri/csv` is a fast and easy-to-use library to read and write CSV files.

## Quick Example

### Reading a CSV File
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

### Writing a CSV File
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

<div style="overflow-x: auto;">

| Format                                      | Line Termination             | Variable Number of Fields<sup>1</sup> | Quotes in Unqualified Fields<sup>2</sup> | Extra Text After Quoted Field<sup>3</sup> | Truncated Quoted Fields<sup>4</sup> |
|:--------------------------------------------|:-----------------------------|:-------------------------------------:|:----------------------------------------:|:-----------------------------------------:|:-----------------------------------:|
| **Machintosh<sup>5</sup>**                  | `\r`                         |                                       |                    no                    |                    no                     |                 no                  |
| **RFC&nbsp;4180&nbsp;"Strict"**             | `\r\n`                       |                                       |                    no                    |                    no                     |                 no                  |
| **RFC&nbsp;4180&nbsp;"Half&nbsp;Relaxed"**  | `\r\n`,&nbsp;`\n`            |                                       |                                          |                    no                     |                 no                  |
| **RFC&nbsp;4180&nbsp;"Fully&nbsp;Relaxed"** | `\r\n`,&nbsp;`\r`,&nbsp;`\n` |                                       |                                          |                                           |                                     |
| **MS&nbsp;Excel**                           | `\r\n`,&nbsp;`\r`,&nbsp;`\n` |                                       |                                          |                                           |                                     |

</div>
<small>

**Variable Number of Fields<sup>1</sup>**: the format accepts files containing a different number of fields on
different lines.<br/>
**Quotes in Unqualified Fields<sup>2</sup>**: the format accepts unqualified fields containing double quotes (`"`);
for example, the line `aaa,b"b"b,ccc CRLF` is interpreted as ⟨`aaa`, `b"b"b`, `ccc`⟩.<br/>
**Extra Text After Quoted Field<sup>3</sup>**: the format accepts free text after the closing double quotes (`"`) of
a qualified field; for example, the line `aaa,"bb"b,ccc` is interpreted as ⟨`aaa`, `bbb`, `ccc`⟩.<br/>
**Truncated Quoted Fields<sup>4</sup>**: the format accepts a field starting with a double quote character (`"`)
but the end of file is reached prior to the corresponding closing double quote; for example, the line
`aaa,bbb,"ccc EOF` is interpreted as ⟨`aaa`, `bbb`, `ccc`⟩.<br/>
**Macintosh<sup>5</sup>**: refers to the legacy line-termination convention (`\r`) used by classic Mac OS systems
prior to the
transition to Unix-based OS X in 2001.

</small>

The format can be defined at the creation of a `CsvReader`.
```java
try (CsvReader<Contact> reader = CsvReader.open(new File("contacts.csv"), StandardCharsets.UTF_8, Rfc4180FullyRelaxedFormat.provider(), new ContactDeserializer())){
    while (reader.hasNext()) {
        Contact contact = reader.next();
        // ...
    }
}
```

## Vector API
`mneri/csv` features an alternative high-performance parser implementation built on top of Java's **Vector API**. By 
everaging SIMD (Single Instruction, Multiple Data) CPU instructions (such as AVX or NEON), this parser can process
chunks of data concurrently in a single CPU cycle, significantly lowering parsing time.

Because the Vector API is an incubating feature in Java (available from **Java 16 and later**), it is hidden behind an
incubator module. The Vector API can be enabled via the JVM flag `--add-modules jdk.incubator.vector`.

## Performances

The preliminary results are excellent.

| Parser                   | Benchmark            | Average Time             |
|:-------------------------|:---------------------|:-------------------------|
| `sesseltjonna`           | `worldcitiespop.csv` | 327.306 ± 20.752  ms/op  |
| `mneri/csv` (Vector API) | `worldcitiespop.csv` | 467.593 ± 20.892  ms/op  |
| `FastCsv`                | `worldcitiespop.csv` | 501.092 ± 14.628  ms/op  |
| `SimpleFlatMapper`       | `worldcitiespop.csv` | 506.456 ± 13.523  ms/op  |
| `picocsv`                | `worldcitiespop.csv` | 537.334 ± 15.956  ms/op  |
| `Univocity`              | `worldcitiespop.csv` | 546.224 ±  9.908  ms/op  |
| `mneri/csv` (sequential) | `worldcitiespop.csv` | 589.560 ± 12.901  ms/op  |
| `OpenCSV`                | `worldcitiespop.csv` | 1225.205 ± 33.525  ms/op |
| `Apache Commons CSV`     | `worldcitiespop.csv` | 2696.596 ± 33.325  ms/op |
