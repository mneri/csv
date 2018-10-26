package me.mneri.csv.test.serialization;

import me.mneri.csv.CsvSerializer;
import me.mneri.csv.test.model.Person;

import java.util.Date;
import java.util.List;

public class PersonSerializer implements CsvSerializer<Person> {
    @Override
    public void serialize(Person person, List<String> out) {
        out.add(person.getFirstName());
        out.add(person.getMiddleName());
        out.add(person.getLastName());
        out.add(person.getNickname());

        Date birthDate = person.getBirthDate();

        if (birthDate != null) {
            out.add(Long.toString(birthDate.getTime()));
        } else {
            out.add(null);
        }

        out.add(person.getAddress());
        out.add(person.getWebsite());
    }
}
