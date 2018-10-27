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
        if (object == null) {
            return false;
        }

        if (!(object instanceof Person)) {
            return false;
        }

        Person other = (Person) object;

        //@formatter:off
        if (!Objects.equals(getAddress(),    other.getAddress()))    { return false; }
        if (!Objects.equals(getBirthDate(),  other.getBirthDate()))  { return false; }
        if (!Objects.equals(getFirstName(),  other.getFirstName()))  { return false; }
        if (!Objects.equals(getLastName(),   other.getLastName()))   { return false; }
        if (!Objects.equals(getMiddleName(), other.getMiddleName())) { return false; }
        if (!Objects.equals(getNickname(),   other.getNickname()))   { return false; }
        //@formatter:on

        return Objects.equals(getWebsite(), other.getWebsite());
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
