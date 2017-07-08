package me.mneri.csv.test;

import java.util.Date;

public class Person {
    private String address;
    private Date birthDate;
    private String firstName;
    private String lastName;
    private String middleName;
    private String nickname;
    private String website;

    public Person() {
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;

        if (object == null)
            return false;

        if (getClass() != object.getClass())
            return false;

        Person other = (Person) object;

        if (address == null) {
            if (other.address != null)
                return false;
        } else {
            if (!address.equals(other.address))
                return false;
        }

        if (birthDate == null) {
            if (other.birthDate != null)
                return false;
        } else {
            if (!birthDate.equals(other.birthDate))
                return false;
        }

        if (firstName == null) {
            if (other.firstName != null)
                return false;
        } else {
            if (!firstName.equals(other.firstName))
                return false;
        }

        if (lastName == null) {
            if (other.lastName != null)
                return false;
        } else {
            if (!lastName.equals(other.lastName))
                return false;
        }

        if (middleName == null) {
            if (other.middleName != null)
                return false;
        } else {
            if (!middleName.equals(other.middleName))
                return false;
        }

        if (nickname == null) {
            if (other.nickname != null)
                return false;
        } else {
            if (!nickname.equals(other.nickname))
                return false;
        }

        if (website == null) {
            if (other.website != null)
                return false;
        } else {
            if (!website.equals(other.website))
                return false;
        }

        return true;
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
        int result = address != null ? address.hashCode() : 0;

        result = 31 * result + (birthDate != null ? birthDate.hashCode() : 0);
        result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
        result = 31 * result + (middleName != null ? middleName.hashCode() : 0);
        result = 31 * result + (nickname != null ? nickname.hashCode() : 0);
        result = 31 * result + (website != null ? website.hashCode() : 0);

        return result;
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
        return String.format("[%s, %s, %s, %s, %s, %s, %s]", firstName, middleName, lastName, nickname,
                birthDate, address, website);
    }
}
