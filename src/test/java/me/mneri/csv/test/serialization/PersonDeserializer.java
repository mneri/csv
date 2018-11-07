/*
 * This file is part of mneri/csv.
 *
 * mneri/csv is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mneri/csv is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with mneri/csv. If not, see <http://www.gnu.org/licenses/>.
 */

package me.mneri.csv.test.serialization;

import me.mneri.csv.CsvDeserializer;
import me.mneri.csv.RecyclableCsvLine;
import me.mneri.csv.test.model.Person;

import java.util.Date;

public class PersonDeserializer implements CsvDeserializer<Person> {
    @Override
    public Person deserialize(RecyclableCsvLine line) {
        Person person = new Person();

        person.setFirstName(line.getString(0));
        person.setMiddleName(line.getString(1));
        person.setLastName(line.getString(2));
        person.setNickname(line.getString(3));

        String number = line.getString(4);

        if (number != null) {
            person.setBirthDate(new Date(Long.parseLong(number)));
        }

        person.setAddress(line.getString(5));
        person.setWebsite(line.getString(6));

        return person;
    }
}
