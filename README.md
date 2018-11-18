# mneri/csv
`mneri/csv` is a fast and easy-to-use library to read and write CSV files.

[![Build Status](https://travis-ci.org/mneri/csv.svg?branch=master)](https://travis-ci.org/mneri/csv)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/d30b8cc221234302a0f4686cd9a38f42)](https://app.codacy.com/app/mneri_2/csv?utm_source=github.com&utm_medium=referral&utm_content=mneri/csv&utm_campaign=Badge_Grade_Dashboard)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/mneri/csv.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/mneri/csv/context:java)
[![Total alerts](https://img.shields.io/lgtm/alerts/g/mneri/csv.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/mneri/csv/alerts/)
[![Coverage Status](https://coveralls.io/repos/github/mneri/csv/badge.svg?branch=master)](https://coveralls.io/github/mneri/csv?branch=master)

## Motivation
The code of most of the parsers you can find online is bloated and complicated. I wanted make a parser that was brief,
clean and easy to understand. The algorithm of `CsvReader` is less than
[50 lines of code](https://github.com/mneri/csv/blob/master/src/main/java/me/mneri/csv/CsvReader.java#L171).

## Performances
It is fast. My preliminary tests show that the speed of `CsvReader` is comparable to the speed of
[uniVocity](https://github.com/uniVocity/univocity-parsers), one of the fastest csv parsers. I submitted a
[pull request](https://github.com/uniVocity/csv-parsers-comparison/pull/23) to their benchmark, yet to be approved.

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
public class PersonDeserializer implements CsvDeserializer<Person> {
    @Override
    public Person deserialize(RecyclableCsvLine line) {
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

You can get a Java 8 `Stream` from a CSV file you using `CsvReader#stream()` static method.

```java
try (Stream<Person> stream = CsvReader.stream(new File("test.csv"), new PersonDeserializer())) {
   stream.forEach(System.out::println);
}
```
