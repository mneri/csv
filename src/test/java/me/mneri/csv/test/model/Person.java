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

package me.mneri.csv.test.model;

import java.util.Date;
import java.util.Objects;

public class Person implements Cloneable {
    private String address;
    private Date birthDate;
    private String firstName;
    private String lastName;
    private String middleName;
    private String nickname;
    private String website;

    @Override
    public Person clone() {
        try {
            Person clone = (Person) super.clone();

            clone.address = address;
            clone.birthDate = (Date) birthDate.clone();
            clone.firstName = firstName;
            clone.lastName = lastName;
            clone.middleName = middleName;
            clone.nickname = nickname;
            clone.website = website;

            return clone;
        } catch (CloneNotSupportedException ignored) {
            return null;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }

        if (object == null) {
            return false;
        }

        if (!(object instanceof Person)) {
            return false;
        }

        Person other = (Person) object;

        //@formatter:off
        return Objects.equals(getAddress(),    other.getAddress())    &&
               Objects.equals(getBirthDate(),  other.getBirthDate())  &&
               Objects.equals(getFirstName(),  other.getFirstName())  &&
               Objects.equals(getLastName(),   other.getLastName())   &&
               Objects.equals(getMiddleName(), other.getMiddleName()) &&
               Objects.equals(getNickname(),   other.getNickname())   &&
               Objects.equals(getWebsite(),    other.getWebsite());
        //@formatter:on
    }

    public String getAddress() {
        return address;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getNickname() {
        return nickname;
    }

    public String getWebsite() {
        return website;
    }

    @Override
    public int hashCode() {
        //@formatter:off
        return Objects.hash(getAddress(),
                            getBirthDate(),
                            getFirstName(),
                            getLastName(),
                            getMiddleName(),
                            getNickname(),
                            getWebsite());
        //@formatter:on
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    @Override
    public String toString() {
        //@formatter:off
        return String.format("['%s', '%s', '%s', '%s', '%s', '%s', '%s']",
                             getFirstName(),
                             getMiddleName(),
                             getLastName(),
                             getNickname(),
                             getBirthDate(),
                             getAddress(),
                             getWebsite());
        //@formatter:on
    }
}
