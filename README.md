# mneri/csv
`mneri/csv` is a fast and easy-to-use library to read and write CSV files.

Memory consumption is also low. You can run `mneri/csv` on uniVocity benchmark with a 1MB JVM (`-Xmx1m`).

## Example
To read a CSV file you use `CsvReader` class.

```java
try (CsvReader<Person> reader = CsvReader.open(new File("people.csv"), new PersonDeserializer())) {
    while (reader.hasNext()) {
        doSomething(reader.next());
    }
}
```

Where `PersonDeserializer` is:

```java
public class PersonDeserializer implements Deserializer<Person> {
    @Override
    public Person deserialize(RecycledCsvLine line) {
        Person person = new Person();
        person.setFirstName(line.getString(0));
        person.setLastName(line.getString(1));
        return person;
    }
}
```

Writing to a csv file is easy, too.

```java
try (CsvWriter<Person> writer = CsvWriter.open(new File("people.csv"), new PersonSerializer())) {
    for (Person person : persons) {
        writer.put(person);
    }
}
```

Where `PersonSerializer` is:

```java
public class PersonSerializer implements CsvSerializer<Person> {
    @Override
    public void serialize(Person person, List<String> out) {
        out.add(person.getFirstName());
        out.add(person.getLastName());
    }
}
```
