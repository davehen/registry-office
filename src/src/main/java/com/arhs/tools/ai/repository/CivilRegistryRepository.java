package com.arhs.tools.ai.repository;

import com.arhs.tools.ai.domain.Person;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CivilRegistryRepository extends CrudRepository<Person, Integer> {

    Optional<Person> findByUserName(String userName);

    Optional<Person> findByEmail(String email);

    void deleteByEmail(String email);

    @Query("SELECT p FROM Person p WHERE " +
           "LOWER(p.userName) LIKE LOWER(CONCAT('%', :text, '%')) OR " +
           "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :text, '%')) OR " +
           "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :text, '%')) OR " +
           "LOWER(p.email) LIKE LOWER(CONCAT('%', :text, '%'))")
    List<Person> searchByText(@Param("text") String text);

}
