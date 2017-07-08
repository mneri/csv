package me.mneri.csv.test;

import me.mneri.csv.CsvConverter;

import java.util.Date;
import java.util.List;

public class PersonConverter implements CsvConverter<Person> {
    @Override
    public Person toObject(List<String> line) {
        Person person = new Person();

        person.setFirstName(line.get(0));
        person.setMiddleName(line.get(1));
        person.setLastName(line.get(2));
        person.setNickname(line.get(3));

        String number = line.get(4);

        if (number != null)
            person.setBirthDate(new Date(Long.parseLong(number)));

        person.setAddress(line.get(5));
        person.setWebsite(line.get(6));

        return person;
    }

    @Override
    public void toCsvLine(Person person, List<String> out) {
        out.add(person.getFirstName());
        out.add(person.getMiddleName());
        out.add(person.getLastName());
        out.add(person.getNickname());

        Date birthDate = person.getBirthDate();

        if (birthDate != null)
            out.add(Long.toString(birthDate.getTime()));
        else
            out.add(null);

        out.add(person.getAddress());
        out.add(person.getWebsite());
    }
}
