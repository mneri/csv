# mneri/csv
`mneri/csv` is a fast and easy-to-use library to read and write CSV files.

## Example
Let `Person` be a POJO.

    public class Person {
        private String firstName;
        private String lastName;
        // Getters and setters
    }

We first need to define a `CsvConverter`. Converters have two methods: `toObject()` used to define object creation
starting from a CSV line and `toCsvLine()` used to convert an object to a CSV line.

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

    try (CsvWriter writer = CsvWriter.open(new File("test.csv"), new PersonConverter())) {
        for (Person person : persons)
            writer.writeLine(person);
    }

To read from a CSV file you use `CsvReader` class.

    try (CsvReader reader = CsvReader.open(new File("test.csv"), new PersonConverter())) {
        Person person;
        while ((person = reader.readLine()) != null)
            persons.add(person);
    }
