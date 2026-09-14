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

## Support for Different CSV Dialects

<div style="overflow-x: auto;">

| Format                                      | Line Termination             | Variable Number of Fields[^1] | Quotes in Unqualified Fields[^2] | Extra Text After Quoted Field[^3] | Truncated Quoted Fields[^4] |
|:--------------------------------------------|:-----------------------------|:-----------------------------:|:--------------------------------:|:---------------------------------:|:---------------------------:|
| **Machintosh**[^5]                          | `\r`                         |                               |                ❌                 |                 ❌                 |              ❌              |
| **RFC&nbsp;4180&nbsp;"Strict"**             | `\r\n`                       |                               |                ❌                 |                 ❌                 |              ❌              |
| **RFC&nbsp;4180&nbsp;"Half&nbsp;Relaxed"**  | `\r\n`,&nbsp;`\n`            |                               |                                  |                 ❌                 |              ❌              |
| **RFC&nbsp;4180&nbsp;"Fully&nbsp;Relaxed"** | `\r\n`,&nbsp;`\r`,&nbsp;`\n` |                               |                                  |                                   |                             |
| **MS&nbsp;Excel**                           | `\r\n`,&nbsp;`\r`,&nbsp;`\n` |                               |                                  |                                   |                             |

</div>

[^1]: *Variable Number of Fields* 
[^2]: *Quotes in Unqualified Fields*
[^3]: *Extra Text After Quoted Field*
[^4]: *Truncated Quoted Fields*
[^5]: **Macintosh** refers to the legacy line-termination convention (`\r`) used by classic Mac OS systems prior to the
transition to Unix-based OS X in 2001.

## Performances