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

| Format                                                                 | Line Termination                                             | Variable Number of Fields | Quotes in Unqualified Fields | Extra Text After Quoted Field | Truncated Quoted Fields |
|:-----------------------------------------------------------------------|:-------------------------------------------------------------|:-------------------------:|:----------------------------:|:-----------------------------:|:-----------------------:|
| <span style="white-space: nowrap;">**Machintosh***</span>              | <span style="white-space: nowrap;">`\r`</span>               |                           |              ❌               |               ❌               |            ❌            |
| <span style="white-space: nowrap;">**RFC 4180 "Strict"**</span>        | <span style="white-space: nowrap;">`\r\n`</span>             |                           |              ❌               |               ❌               |            ❌            |
| <span style="white-space: nowrap;">**RFC 4180 "Half Relaxed"**</span>  | <span style="white-space: nowrap;">`\r\n`, `\n`</span>       |                           |                              |               ❌               |            ❌            |
| <span style="white-space: nowrap;">**RFC 4180 "Fully Relaxed"**</span> | <span style="white-space: nowrap;">`\r\n`, `\r`, `\n`</span> |                           |                              |                               |                         |
| <span style="white-space: nowrap;">**MS Excel**</span>                 | <span style="white-space: nowrap;">`\r\n`, `\r`, `\n`</span> |                           |                              |                               |                         |

*\* **Macintosh**: Refers to the legacy line-termination convention (`\r`) used by classic Mac OS systems prior to the
transition to Unix-based OS X in 2001.*

## Performances