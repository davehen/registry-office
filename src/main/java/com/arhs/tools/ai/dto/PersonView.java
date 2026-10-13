package com.arhs.tools.ai.dto;

import com.arhs.tools.ai.domain.Person;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(name = "Person")
public record PersonView(int id, String userName, String firstName, String lastName,
                         String email, LocalDate birthDate, String nationality,
                         Person.Status status, Integer mergedIntoPersonId, long version) {
    public static PersonView of(Person p) {
        return new PersonView(p.getId(), p.getUserName(), p.getFirstName(), p.getLastName(),
                p.getEmail(), p.getBirthDate(), p.getNationality(), p.getStatus(),
                p.getMergedIntoPersonId(), p.getVersion());
    }
}
