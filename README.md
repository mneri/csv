# mneri/csv

`mneri/csv` is a fast and easy-to-use library to read and write CSV files.

## Quick Example

### Reading a CSV File

```java
try(CsvReader<Contact> reader=CsvReader.open(new File("contacts.csv"),StandardCharsets.UTF_8,new ContactDeserializer())){
        while(reader.hasNext()){
        Contact contact=reader.next(); // Records are mapped to domain objects via the provided ContactDeserializer
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

Writing to a csv file is easy, too.

```java
try(CsvWriter<Contact> writer=CsvWriter.open(new File("contacts.csv"),StandardCharsets.UTF_8,new ContactSerializer())){
        for(Contact contact:contacts){
        writer.write(contact); // Domain objects are mapped to records via the provided ContactSerializer
        }
        }
```

Where `PersonSerializer` is:

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

## Performances