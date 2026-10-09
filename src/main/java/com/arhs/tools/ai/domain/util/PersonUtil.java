package com.arhs.tools.ai.domain.util;

import com.arhs.tools.ai.domain.Person;

import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PersonUtil {

    public static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{4,32}$");
    public static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z ]{1,32}$");
    public static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$");

    public static final int MIN_AGE = 5;
    public static final int ADULT_AGE = 18;
    public static final int MAX_AGE = 60;

    public static void checkUsernameIsValid(Person person) {
        if (person.getUserName() == null)
            throw new IllegalArgumentException("Username cannot be null");
        Matcher matcher = USERNAME_PATTERN.matcher(person.getUserName());
        if (!matcher.find())
            throw new IllegalArgumentException("Invalid username '" + person.getUserName() + "'");
    }

    public static void checkFirstnameIsValid(Person person) {
        if (person.getFirstName() == null)
            throw new IllegalArgumentException("First name cannot be null");
        Matcher matcher = NAME_PATTERN.matcher(person.getFirstName());
        if (!matcher.find())
            throw new IllegalArgumentException("Invalid first name '" + person.getFirstName() + "'");
    }

    public static void checkLastnameIsValid(Person person) {
        if (person.getLastName() == null)
            throw new IllegalArgumentException("Last name cannot be null");
        Matcher matcher = NAME_PATTERN.matcher(person.getLastName());
        if (!matcher.find())
            throw new IllegalArgumentException("Invalid last name '" + person.getLastName() + "'");
    }

    public static void checkEMailIsValid(Person person) {
        if (person.getEmail() == null)
            throw new IllegalArgumentException("E-mail cannot be null");
        Matcher matcher = EMAIL_PATTERN.matcher(person.getEmail());
        if (!matcher.find())
            throw new IllegalArgumentException("Invalid e-mail '" + person.getEmail() + "'");
    }

    public static void checkBirthDateIsValid(Person person) {
        if (person.getBirthDate() == null)
            throw new IllegalArgumentException("Birth date cannot be null");

        if (isYoungerThan(person, MIN_AGE) || isOlderThan(person, MAX_AGE))
            throw new IllegalArgumentException("Age must be between " + MIN_AGE + " and " + MAX_AGE);
    }

    public static void check(Person person) {
        checkUsernameIsValid(person);
        checkFirstnameIsValid(person);
        checkLastnameIsValid(person);
        checkEMailIsValid(person);
        checkBirthDateIsValid(person);
    }

    public static boolean isMinor(Person person) {
        return isYoungerThan(person, ADULT_AGE);
    }

    public static boolean isAdult(Person person) {
        return isOlderThan(person, ADULT_AGE);
    }

    public static boolean isYoungerThan(Person person, int age) {
        return calculateAge(person) < age;
    }

    public static boolean isYoungerOrEqualTo(Person person, int age) {
        return calculateAge(person) <= age;
    }

    public static boolean isOlderThan(Person person, int age) {
        return calculateAge(person) > age;
    }

    public static boolean isOlderOrEqualTo(Person person, int age) {
        return calculateAge(person) >= age;
    }

    private static int calculateAge(Person person) {
        return Period.between(person.getBirthDate(), LocalDate.now()).getYears();
    }

}
