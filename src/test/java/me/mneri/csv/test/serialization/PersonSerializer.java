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
