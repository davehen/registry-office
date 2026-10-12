package com.arhs.tools.ai.repository;

import com.arhs.tools.ai.domain.Person;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CivilRegistryRepositoryTest {
    @Autowired
    private CivilRegistryRepository repository;

    @Test
    void nationalityQueryIsExactCaseInsensitiveAndExcludesNulls() {
        save("one", "French");
        save("two", "FRENCH");
        save("three", "German");
        save("four", null);
        assertThat(repository.findByNationalityIgnoreCase("french"))
                .extracting(Person::getUserName).containsExactlyInAnyOrder("one", "two");
        assertThat(repository.findByNationalityIgnoreCase("Fren")).isEmpty();
        assertThat(repository.findByNationalityIgnoreCase("Spanish")).isEmpty();
    }

    private void save(String username, String nationality) {
        Person person = new Person(username, "First", "Last", username + "@example.test", LocalDate.of(1990, 1, 1));
        person.setNationality(nationality);
        repository.save(person);
    }
}
