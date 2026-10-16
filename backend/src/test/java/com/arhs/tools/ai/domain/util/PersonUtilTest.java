package com.arhs.tools.ai.domain.util;

import com.arhs.tools.ai.domain.Person;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PersonUtilTest {

    @ParameterizedTest
    @ValueSource(strings = {"user123", "admin_user", "longusername123456789012345678"})
    void checkUsernameIsValid_ValidUsernames_ShouldNotThrowException(String username) {
        Person person = new Person(username, "John", "Doe", "john.doe@example.com", LocalDate.now().minusYears(20));
        assertDoesNotThrow(() -> PersonUtil.checkUsernameIsValid(person));
    }

    @ParameterizedTest
    @ValueSource(strings = {"usr", "thisusernameistoolongtobevaild123456789", "user@name", "user name"})
    void checkUsernameIsValid_InvalidUsernames_ShouldThrowException(String username) {
        Person person = new Person(username, "John", "Doe", "john.doe@example.com", LocalDate.now().minusYears(20));
        assertThrows(IllegalArgumentException.class, () -> PersonUtil.checkUsernameIsValid(person));
    }

    @Test
    void checkUsernameIsValid_NullUsername_ShouldThrowException() {
        Person person = new Person(null, "John", "Doe", "john.doe@example.com", LocalDate.now().minusYears(20));
        assertThrows(IllegalArgumentException.class, () -> PersonUtil.checkUsernameIsValid(person));
    }

    @ParameterizedTest
    @ValueSource(strings = {"John", "John Doe", "A"})
    void checkFirstnameIsValid_ValidFirstnames_ShouldNotThrowException(String firstname) {
        Person person = new Person("user123", firstname, "Doe", "john.doe@example.com", LocalDate.now().minusYears(20));
        assertDoesNotThrow(() -> PersonUtil.checkFirstnameIsValid(person));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "John123", "Thisfirstnameistoolongtobevaild123456789"})
    void checkFirstnameIsValid_InvalidFirstnames_ShouldThrowException(String firstname) {
        Person person = new Person("user123", firstname, "Doe", "john.doe@example.com", LocalDate.now().minusYears(20));
        assertThrows(IllegalArgumentException.class, () -> PersonUtil.checkFirstnameIsValid(person));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Doe", "Doe Smith", "D"})
    void checkLastnameIsValid_ValidLastnames_ShouldNotThrowException(String lastname) {
        Person person = new Person("user123", "John", lastname, "john.doe@example.com", LocalDate.now().minusYears(20));
        assertDoesNotThrow(() -> PersonUtil.checkLastnameIsValid(person));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Doe123", "Thislastnameistoolongtobevaild123456789"})
    void checkLastnameIsValid_InvalidLastnames_ShouldThrowException(String lastname) {
        Person person = new Person("user123", "John", lastname, "john.doe@example.com", LocalDate.now().minusYears(20));
        assertThrows(IllegalArgumentException.class, () -> PersonUtil.checkLastnameIsValid(person));
    }

    @ParameterizedTest
    @ValueSource(strings = {"john.doe@example.com", "user@domain.co", "first.last@sub.domain.org"})
    void checkEMailIsValid_ValidEmails_ShouldNotThrowException(String email) {
        Person person = new Person("user123", "John", "Doe", email, LocalDate.now().minusYears(20));
        assertDoesNotThrow(() -> PersonUtil.checkEMailIsValid(person));
    }

    @ParameterizedTest
    @ValueSource(strings = {"plainaddress", "#@%^%#$@#$@#.com", "@example.com", "Joe Smith <email@example.com>", "email.example.com", "email@example@example.com"})
    void checkEMailIsValid_InvalidEmails_ShouldThrowException(String email) {
        Person person = new Person("user123", "John", "Doe", email, LocalDate.now().minusYears(20));
        assertThrows(IllegalArgumentException.class, () -> PersonUtil.checkEMailIsValid(person));
    }

    @Test
    void checkBirthDateIsValid_ValidDate_ShouldNotThrowException() {
        Person person = new Person("user123", "John", "Doe", "john.doe@example.com", LocalDate.now().minusYears(20));
        assertDoesNotThrow(() -> PersonUtil.checkBirthDateIsValid(person));
    }

    @Test
    void checkBirthDateIsValid_TooYoung_ShouldThrowException() {
        Person person = new Person("user123", "John", "Doe", "john.doe@example.com", LocalDate.now().minusYears(PersonUtil.MIN_AGE - 1));
        assertThrows(IllegalArgumentException.class, () -> PersonUtil.checkBirthDateIsValid(person));
    }

    @Test
    void checkBirthDateIsValid_TooOld_ShouldThrowException() {
        Person person = new Person("user123", "John", "Doe", "john.doe@example.com", LocalDate.now().minusYears(PersonUtil.MAX_AGE + 1));
        assertThrows(IllegalArgumentException.class, () -> PersonUtil.checkBirthDateIsValid(person));
    }

    @Test
    void isMinor_MinorPerson_ShouldReturnTrue() {
        Person person = new Person("user123", "John", "Doe", "john.doe@example.com", LocalDate.now().minusYears(PersonUtil.ADULT_AGE - 1));
        assertTrue(PersonUtil.isMinor(person));
    }

    @Test
    void isMinor_AdultPerson_ShouldReturnFalse() {
        Person person = new Person("user123", "John", "Doe", "john.doe@example.com", LocalDate.now().minusYears(PersonUtil.ADULT_AGE));
        assertFalse(PersonUtil.isMinor(person));
    }

    @Test
    void isAdult_AdultPerson_ShouldReturnTrue() {
        Person person = new Person("user123", "John", "Doe", "john.doe@example.com", LocalDate.now().minusYears(PersonUtil.ADULT_AGE + 1));
        assertTrue(PersonUtil.isAdult(person));
    }

    @Test
    void isAdult_MinorPerson_ShouldReturnFalse() {
        Person person = new Person("user123", "John", "Doe", "john.doe@example.com", LocalDate.now().minusYears(PersonUtil.ADULT_AGE - 1));
        assertFalse(PersonUtil.isAdult(person));
    }

    @Test
    void ageComparisons_ShouldWorkCorrectly() {
        Person person = new Person("user123", "John", "Doe", "john.doe@example.com", LocalDate.now().minusYears(20));
        
        assertTrue(PersonUtil.isYoungerThan(person, 21));
        assertFalse(PersonUtil.isYoungerThan(person, 19));
        
        assertTrue(PersonUtil.isYoungerOrEqualTo(person, 20));
        assertTrue(PersonUtil.isYoungerOrEqualTo(person, 21));
        assertFalse(PersonUtil.isYoungerOrEqualTo(person, 19));
        
        assertTrue(PersonUtil.isOlderThan(person, 19));
        assertFalse(PersonUtil.isOlderThan(person, 20));
        
        assertTrue(PersonUtil.isOlderOrEqualTo(person, 20));
        assertTrue(PersonUtil.isOlderOrEqualTo(person, 19));
        assertFalse(PersonUtil.isOlderOrEqualTo(person, 21));
    }
}