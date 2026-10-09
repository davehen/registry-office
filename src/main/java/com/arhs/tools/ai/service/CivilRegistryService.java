package com.arhs.tools.ai.service;

import com.arhs.tools.ai.domain.Person;
import com.arhs.tools.ai.exception.DuplicatePropertyException;

import java.util.List;
import java.util.Optional;

public interface CivilRegistryService {

    Optional<Person> find(final int id);

    Optional<Person> findByUserName(final String userName);

    Optional<Person> findByEmail(final String email);

    List<Person> findAll();

    List<Person> search(final String inputText);

    List<Person> search(final String inputText, final Integer ageFrom, final Integer ageTo);

    List<Person> findMinors();

    List<Person> findAdults();

    Person create(final Person person) throws DuplicatePropertyException;

    Optional<Person> update(final Person person) throws DuplicatePropertyException;

    Optional<Person> delete(final int id);

    Optional<Person> deleteByUserName(final String userName);

    Optional<Person> deleteByEmail(final String email);

    List<Person> deleteAll();
}
