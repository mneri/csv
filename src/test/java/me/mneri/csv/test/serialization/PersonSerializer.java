/*
 * Copyright 2018 Massimo Neri <hello@mneri.me>
 *
 * This file is part of mneri/csv.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.mneri.csv.test.serialization;

import me.mneri.csv.serialize.CsvSerializer;
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
