package com.arhs.tools.ai.service;

import com.arhs.tools.ai.domain.Person;
import com.arhs.tools.ai.domain.util.PersonUtil;
import com.arhs.tools.ai.exception.DuplicatePropertyException;
import com.arhs.tools.ai.exception.RegistryConflictException;
import com.arhs.tools.ai.repository.CivilRegistryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.arhs.tools.ai.domain.util.PersonUtil.MAX_AGE;
import static com.arhs.tools.ai.domain.util.PersonUtil.MIN_AGE;
import static com.arhs.tools.ai.exception.DuplicatePropertyException.UniqueProperty.EMAIL;
import static com.arhs.tools.ai.exception.DuplicatePropertyException.UniqueProperty.USERNAME;

@Service
@Transactional(readOnly = true)
public class CivilRegistryServiceImpl implements CivilRegistryService {

    private final CivilRegistryRepository civilRegistryRepository;

    @Autowired
    public CivilRegistryServiceImpl(final CivilRegistryRepository civilRegistryRepository) {
        this.civilRegistryRepository = civilRegistryRepository;
    }

    @Override
    public Optional<Person> find(final int id) {
        return civilRegistryRepository.findById(id);
    }

    @Override
    public Optional<Person> findByUserName(final String userName) {
        return civilRegistryRepository.findByUserName(userName);
    }

    @Override
    public Optional<Person> findByEmail(final String email) {
        return civilRegistryRepository.findByEmail(email);
    }

    @Override
    public List<Person> findAll() {
        return civilRegistryRepository.findByStatus(Person.Status.ACTIVE);
    }

    @Override
    public List<Person> search(final String inputText) {
        return search(inputText, null, null);
    }

    @Override
    public List<Person> findByNationality(final String nationality) {
        if (nationality == null || nationality.isBlank()) {
            throw new IllegalArgumentException("Nationality must not be blank");
        }
        return civilRegistryRepository.findByNationalityIgnoreCase(nationality.trim());
    }

    @Override
    public List<Person> search(final String inputText, final Integer ageFrom, final Integer ageTo) {
        String myInputText = inputText == null ? "" : inputText;
        List<Person> persons = new ArrayList<>(civilRegistryRepository.searchByText(myInputText));
        int myAgeFrom = ageFrom == null ? MIN_AGE : ageFrom;
        int myAgeTo = ageTo == null ? MAX_AGE : ageTo;
        return persons.stream()
                .filter(p -> PersonUtil.isOlderOrEqualTo(p, myAgeFrom))
                .filter(p -> PersonUtil.isYoungerOrEqualTo(p, myAgeTo))
                .toList();
    }

    // note: this method could have been avoided by simply using method CivilRegistryRepository.findByAgeLessThan.
    // however, it has been decided to implement an explicit logic to show what unit tests shall be put in place to cover it
    // see also: PersonUtilUnitTest
    @Override
    public List<Person> findMinors() {
        return findAll().stream()
                .filter(PersonUtil::isMinor)
                .toList();
    }

    // note: this method could have been avoided by simply using method CivilRegistryRepository.findByAgeGreaterThanEqual.
    // however, it has been decided to implement an explicit logic to show what unit tests shall be put in place to cover it
    // see also: PersonUtilUnitTest
    public List<Person> findAdults() {
        return findAll().stream()
                .filter(PersonUtil::isAdult)
                .toList();
    }

    @Override
    @Transactional
    public Person create(final Person person) throws DuplicatePropertyException {
        if (findByUserName(person.getUserName()).isPresent())
            throw new DuplicatePropertyException(USERNAME);
        if (findByEmail(person.getEmail()).isPresent())
            throw new DuplicatePropertyException(EMAIL);

        // Copy only business fields; IDs and lifecycle fields are server-owned.
        return civilRegistryRepository.save(new Person(person));
    }

    @Override
    @Transactional
    public Optional<Person> update(final Person person) throws DuplicatePropertyException {
        var foundPerson = find(person.getId());
        if (foundPerson.isEmpty())
            return Optional.empty();
        requireActive(foundPerson.get());

        var foundPersonWithUserName = findByUserName(person.getUserName());
        if (foundPersonWithUserName.isPresent() && !foundPersonWithUserName.get().equals(foundPerson.get()))
            throw new DuplicatePropertyException(USERNAME);
        var foundPersonWithSameEmail = findByEmail(person.getEmail());
        if (foundPersonWithSameEmail.isPresent() && !foundPersonWithSameEmail.get().equals(foundPerson.get()))
            throw new DuplicatePropertyException(EMAIL);

        foundPerson.get().setUserName(person.getUserName());
        foundPerson.get().setFirstName(person.getFirstName());
        foundPerson.get().setLastName(person.getLastName());
        foundPerson.get().setEmail(person.getEmail());
        foundPerson.get().setBirthDate(person.getBirthDate());
        foundPerson.get().setNationality(person.getNationality());
        return Optional.of(civilRegistryRepository.save(foundPerson.get()));
    }

    @Override
    @Transactional
    public Optional<Person> delete(final int id) {
        var foundPerson = find(id);
        if (foundPerson.isEmpty()) {
            return Optional.empty();
        }
        requireActive(foundPerson.get());

        civilRegistryRepository.delete(foundPerson.get());
        return foundPerson;
    }

    @Override
    @Transactional
    public Optional<Person> deleteByUserName(final String userName) {
        var foundPerson = findByUserName(userName);
        if (foundPerson.isEmpty()) {
            return Optional.empty();
        }

        civilRegistryRepository.delete(foundPerson.get());
        return foundPerson;
    }

    @Override
    @Transactional
    public Optional<Person> deleteByEmail(final String email) {
        var foundPerson = findByEmail(email);
        if (foundPerson.isEmpty()) {
            return Optional.empty();
        }

        civilRegistryRepository.delete(foundPerson.get());
        return foundPerson;
    }

    @Override
    @Transactional
    public List<Person> deleteAll() {
        List<Person> persons = findAll();
        civilRegistryRepository.deleteAll(persons);
        return persons;
    }

    private void requireActive(Person person) {
        if (person.getStatus() != Person.Status.ACTIVE)
            throw new RegistryConflictException("Person has been merged", "person/" + person.getId());
    }
}
