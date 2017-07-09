# mneri/csv
`mneri/csv` is a fast and easy-to-use library to read and write CSV files.

[![Build Status](https://travis-ci.org/mneri/csv.svg?branch=master)](https://travis-ci.org/mneri/csv)

## Example
example Let `Person` be a POJO.

    public class Person {
        private String firstName;
        private String lastName;
        // Getters and setters
    }

We first need to define a `CsvConverter`. Converters have two methods: `toObject()` used to define object creation
starting from a CSV line and `toCsvLine()` used to convert an object to a CSV line. The library handles all the details
of the CSV format and lets you work with plain and clean Java Strings.

    public class PersonConverter implements CsvConverter<Person> {
        @Override
        public Person toObject(List<String> line) {
            Person person = new Person();
            person.setFirstName(line.get(0));
            person.setLastName(line.get(1));
            return person;
        }
        
        @Override
        public void toCsvLine(Person person, List<String> out) {
            out.add(person.getFirstName());
            out.add(person.getLastName());
        }
    }

To write a CSV file you use `CsvWriter` class.

    try (CsvWriter<Person> writer = CsvWriter.open(new File("test.csv"), new PersonConverter())) {
        for (Person person : persons)
            writer.writeLine(person);
    } catch (CsvException | IOException e) {
        e.printStackTrace();
    }

To read from a CSV file you use `CsvReader` class.

    try (CsvReader<Person> reader = CsvReader.open(new File("test.csv"), new PersonConverter())) {
        Person person;

        while ((person = reader.readLine()) != null)
            System.out.println(person);
    } catch (CsvException | IOException e) {
        e.printStackTrace();
    }

To get a Java 8 `Stream` from a CSV file you use `CsvReader#stream()` static method.

    try (Stream<Person> stream = CsvReader.stream(new File("test.csv"), new PersonConverter())) {
        stream.forEach(System.out::println);
    } catch (IOException e) {
        e.printStackTrace();
    }

