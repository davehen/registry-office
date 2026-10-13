package com.arhs.tools.ai.repository;

import com.arhs.tools.ai.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class DuplicatePersistenceTest {
    @Autowired DuplicateCaseRepository cases;
    @Autowired CivilRegistryRepository persons;
    @Autowired PersonMergeAuditRepository audits;
    @Autowired EntityManager entityManager;
    @ParameterizedTest
    @ValueSource(strings = {"2026-10-13T12:25:13.123456789Z", "2026-10-13T12:25:13.999999999Z"})
    void auditTimestampRemainsExactAfterPersistence(String timestamp) {
        Instant input = Instant.parse(timestamp);
        PersonMergeAudit audit = new PersonMergeAudit(1L, 1, 2, input, "Confirmed", "Reviewer", "{}");
        Instant expected = input.truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        assertThat(audit.getMergedAt()).isEqualTo(expected);
        audits.saveAndFlush(audit);
        Long id = audit.getId();
        entityManager.clear(); // Force a database read, not the first-level cache.
        assertThat(audits.findById(id).orElseThrow().getMergedAt()).isEqualTo(expected);
    }
    @Test void orderedPairHasDatabaseUniqueness() {
        Instant now = Instant.now();
        DuplicateCase a = new DuplicateCase(1, 2, now); a.detect(90, List.of("FULL_NAME_BIRTH_DATE"), "a", now);
        cases.saveAndFlush(a);
        DuplicateCase b = new DuplicateCase(1, 2, now); b.detect(90, List.of("FULL_NAME_BIRTH_DATE"), "b", now);
        assertThatThrownBy(() -> cases.saveAndFlush(b)).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void activeQueriesExcludeRetiredRowsWhileIdLookupKeepsTombstone() {
        Person a = new Person("one", "First", "Last", "one@example.test", LocalDate.of(1990, 1, 1)); a.setNationality("French");
        Person b = new Person("two", "First", "Last", "two@example.test", LocalDate.of(1990, 1, 1)); b.setNationality("French");
        persons.saveAndFlush(a); persons.saveAndFlush(b); b.retireInto(a.getId()); persons.flush();
        assertThat(persons.findByStatus(Person.Status.ACTIVE)).extracting(Person::getId).containsExactly(a.getId());
        assertThat(persons.searchByText("First")).extracting(Person::getId).containsExactly(a.getId());
        assertThat(persons.findByNationalityIgnoreCase("french")).extracting(Person::getId).containsExactly(a.getId());
        assertThat(persons.findByUserName("two")).isEmpty(); assertThat(persons.findByEmail("two@example.test")).isEmpty();
        assertThat(persons.findById(b.getId())).isPresent();
    }
}
