package me.mneri.csv.test.serialization;

import me.mneri.csv.CsvDeserializer;
import me.mneri.csv.test.model.Person;

import java.util.Date;
import java.util.List;

public class PersonDeserializer implements CsvDeserializer<Person> {
    @Override
    public Person deserialize(List<String> line) {
        Person person = new Person();

        person.setFirstName(line.get(0));
        person.setMiddleName(line.get(1));
        person.setLastName(line.get(2));
        person.setNickname(line.get(3));

        String number = line.get(4);

        if (number != null) {
            person.setBirthDate(new Date(Long.parseLong(number)));
        }

        person.setAddress(line.get(5));
        person.setWebsite(line.get(6));

        return person;
    }
}
