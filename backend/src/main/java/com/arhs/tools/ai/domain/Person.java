package com.arhs.tools.ai.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.Objects;

@Entity
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Column(unique = true)
    private String userName;
    private String firstName;
    private String lastName;
    @Column(unique = true)
    private String email;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;
    private String nationality;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;
    private Integer mergedIntoPersonId;
    @Version
    private long version;
    // Makes even a merge selecting only canonical values a versioned write.
    private long mergeCount;

    public enum Status { ACTIVE, MERGED }

    public Status getStatus() { return status; }
    public Integer getMergedIntoPersonId() { return mergedIntoPersonId; }
    public long getVersion() { return version; }
    public void recordMerge() { mergeCount++; }
    public void retireInto(int canonicalId) {
        status = Status.MERGED;
        mergedIntoPersonId = canonicalId;
        userName = null;
        email = null;
    }

    protected Person() {
    }

    public Person(String userName, String firstName, String lastName, String email, LocalDate birthDate) {
        setUserName(userName);
        setFirstName(firstName);
        setLastName(lastName);
        setEmail(email);
        setBirthDate(birthDate);
    }

    public Person(Person other) {
        this(other.getUserName(), other.getFirstName(), other.getLastName(), other.getEmail(), other.getBirthDate());
        setNationality(other.getNationality());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) { this.id = id;}

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var person = (Person) o;
        return
                Objects.equals(userName, person.userName) &&
                        Objects.equals(firstName, person.firstName) &&
                        Objects.equals(lastName, person.lastName) &&
                        Objects.equals(email, person.email) &&
                        Objects.equals(birthDate, person.birthDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, firstName, lastName, email, birthDate);
    }

    @Override
    public String toString() {
        return "Person{" +
                "userName='" + userName + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", birthDate='" + birthDate + '\'' +
                '}';
    }
}
