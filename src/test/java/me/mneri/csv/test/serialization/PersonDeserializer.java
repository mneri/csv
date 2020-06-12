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

import me.mneri.csv.deserializer.CsvDeserializer;
import me.mneri.csv.reader.RecyclableCsvLine;
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
