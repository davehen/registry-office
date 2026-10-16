package com.arhs.tools.ai.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PersonTest {
    @Test
    void optionalNationalityAndCopyPreserveSuppliedValue() {
        Person original = new Person("user", "First", "Last", "user@example.test", LocalDate.of(1990, 1, 1));
        assertNull(original.getNationality());
        original.setNationality(" French ");
        assertEquals(" French ", new Person(original).getNationality());
        original.setNationality(null);
        assertNull(new Person(original).getNationality());
    }
}
