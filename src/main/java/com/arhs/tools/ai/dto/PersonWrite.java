package com.arhs.tools.ai.dto;

import com.arhs.tools.ai.domain.Person;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;

// Legacy round-trip clients may send read-only fields; only these business
// fields enter the service. New duplicate operation requests are strict.
@JsonIgnoreProperties(ignoreUnknown = true)
public record PersonWrite(Integer id, String userName, String firstName, String lastName,
                          String email, LocalDate birthDate, String nationality) {
    public Person toPerson() {
        Person p = new Person(userName, firstName, lastName, email, birthDate);
        p.setNationality(nationality);
        if (id != null) p.setId(id);
        return p;
    }
}
