package com.arhs.tools.ai.service;

import com.arhs.tools.ai.domain.Person;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class DuplicateMatcherTest {
    private final DuplicateMatcher matcher = new DuplicateMatcher();
    private Person person(int id, String first, String last, LocalDate date) {
        Person p = new Person("user" + id, first, last, "user" + id + "@example.test", date);
        p.setId(id); return p;
    }
    @ParameterizedTest
    @CsvSource({"  Éloïse O’Neil.-  ,eloise oneil", "JEAN---PAUL,jeanpaul", "O'Neill,oneill"})
    void nameNormalization(String value, String expected) { assertThat(matcher.normalizeName(value)).isEqualTo(expected); }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" \t\n ", "'.-’"})
    void emptyValuesCannotIndependentlyMatch(String value) {
        Person a = person(1, value, value, null), b = person(2, value, value, null);
        a.setUserName(value); b.setUserName(value); a.setEmail(null); b.setEmail(null);
        assertThat(matcher.match(a, b).score()).isZero();
        assertThat(matcher.candidates(List.of(a, b))).isEmpty();
    }
    @Test void emailAndUsernameScore100AndKeepEveryReasonInOrder() {
        LocalDate date = LocalDate.of(1990, 1, 1);
        Person a = person(1, " José  ", "O’Neil", date), b = person(2, "Jose", "o-neil", date);
        a.setUserName("Ó.neil"); b.setUserName("o'neil");
        a.setEmail(" A.B+tag@EXAMPLE.test "); b.setEmail("a.b+tag@example.test");
        var result = matcher.match(a, b);
        assertThat(result.score()).isEqualTo(100);
        assertThat(result.reasons()).containsExactly("EMAIL_EXACT", "USERNAME_EXACT", "FULL_NAME_BIRTH_DATE", "LAST_NAME_FIRST_INITIAL_BIRTH_DATE");
        assertThat(a.getFirstName()).isEqualTo(" José  ");
        assertThat(matcher.normalizeEmail(" A.B+tag@EXAMPLE.test ")).isEqualTo("a.b+tag@example.test");
        assertThat(matcher.candidates(List.of(a, b))).containsExactly(new DuplicateMatcher.Pair(1, 2));
    }
    @Test void fullNameSameDateAndUnicodeInitialRules() {
        LocalDate date = LocalDate.of(1990, 1, 1);
        assertThat(matcher.match(person(1, "Marie", "Curie", date), person(2, "Marie", "Curie", date)).score()).isEqualTo(90);
        // A supplementary Unicode code point is one initial, not one UTF-16 char.
        Person a = person(1, "𐐀alpha", "Last", date), b = person(2, "𐐀beta", "Last", date);
        assertThat(matcher.match(a, b).score()).isEqualTo(75);
        assertThat(matcher.candidates(List.of(a, b))).containsExactly(new DuplicateMatcher.Pair(1, 2));
    }
    @ParameterizedTest @CsvSource({"2020-02-28,2020-02-29", "2020-02-29,2020-03-01", "1999-12-31,2000-01-01"})
    void adjacentCalendarDatesWorkBothDirections(String first, String second) {
        Person a = person(1, "First", "Last", LocalDate.parse(first)), b = person(2, "First", "Last", LocalDate.parse(second));
        assertThat(matcher.match(a, b).score()).isEqualTo(70);
        assertThat(matcher.match(b, a).score()).isEqualTo(70);
        assertThat(matcher.candidates(List.of(b, a))).containsExactly(new DuplicateMatcher.Pair(1, 2));
    }
    @Test void localPartRuleHasMediumScoreAndRequiresDifferentDomains() {
        Person a = person(1, "First", "Last", null), b = person(2, "First", "Last", null);
        a.setEmail(" A.B+tag@one.test "); b.setEmail("a.b+tag@two.test");
        assertThat(matcher.match(a, b).score()).isEqualTo(65);
        assertThat(matcher.match(a, b).reasons()).containsExactly("FULL_NAME_EMAIL_LOCAL_PART");
        assertThat(matcher.candidates(List.of(a, b))).containsExactly(new DuplicateMatcher.Pair(1, 2));
    }
    @ParameterizedTest @ValueSource(strings = {"x", "x@@domain", "@domain", "x@", "x @domain", "x@.", ".x@domain", "x..y@domain"})
    void malformedEmailDoesNotSupplyLocalPartReason(String email) {
        Person a = person(1, "First", "Last", null), b = person(2, "First", "Last", null);
        a.setEmail(email); b.setEmail("x@other.test");
        assertThat(matcher.match(a, b).score()).isZero();
    }
    @Test void candidateIndexIsCompleteDeterministicAndPrunesUnrelatedPeople() {
        List<Person> people = new ArrayList<>();
        people.add(person(1, "First", "Last", LocalDate.of(2000, 1, 1)));
        people.add(person(2, "First", "Last", LocalDate.of(2000, 1, 2)));
        people.add(person(3, "Fiona", "Last", LocalDate.of(2000, 1, 1)));
        people.add(person(4, "Different", "Else", LocalDate.of(1980, 1, 1)));
        people.get(3).setEmail(" USER1@example.test ");
        for (int i = 5; i < 105; i++) people.add(person(i, "First" + i, "Last" + i, LocalDate.of(1970, 1, 1)));
        var pairs = matcher.candidates(people);
        for (int i = 0; i < people.size(); i++) for (int j = i + 1; j < people.size(); j++)
            if (matcher.match(people.get(i), people.get(j)).score() > 0)
                assertThat(pairs).contains(new DuplicateMatcher.Pair(people.get(i).getId(), people.get(j).getId()));
        assertThat(pairs.size()).isLessThan(10);
        Collections.reverse(people);
        assertThat(matcher.candidates(people)).isEqualTo(pairs);
        people.get(people.size() - 1).retireInto(2);
        assertThat(matcher.candidates(people)).noneMatch(pair -> pair.left() == 1 || pair.right() == 1);
    }
    @Test void fingerprintIsDeterministicUnambiguousAndSensitiveToStoredChanges() {
        Person a = person(1, "a|b", "c", null), b = person(2, "other", "name", null);
        String original = matcher.fingerprint(a, b);
        assertThat(original).hasSize(64).isEqualTo(matcher.fingerprint(a, b));
        a.setFirstName("a"); a.setLastName("b|c");
        assertThat(matcher.fingerprint(a, b)).isNotEqualTo(original);
    }
}
