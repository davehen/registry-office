package com.arhs.tools.ai.repository;

import com.arhs.tools.ai.domain.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CivilRegistryRepository extends JpaRepository<Person, Integer> {

    @Query("select p from Person p where p.status = com.arhs.tools.ai.domain.Person$Status.ACTIVE and p.userName = :userName")
    Optional<Person> findByUserName(String userName);

    @Query("select p from Person p where p.status = com.arhs.tools.ai.domain.Person$Status.ACTIVE and p.email = :email")
    Optional<Person> findByEmail(String email);

    @Query("select p from Person p where p.status = com.arhs.tools.ai.domain.Person$Status.ACTIVE and lower(p.nationality) = lower(:nationality)")
    List<Person> findByNationalityIgnoreCase(String nationality);

    List<Person> findByStatus(Person.Status status);

    void deleteByEmail(String email);

    @Query("SELECT p FROM Person p WHERE p.status = com.arhs.tools.ai.domain.Person$Status.ACTIVE AND (" +
           "LOWER(p.userName) LIKE LOWER(CONCAT('%', :text, '%')) OR " +
           "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :text, '%')) OR " +
           "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :text, '%')) OR " +
           "LOWER(p.email) LIKE LOWER(CONCAT('%', :text, '%')))")
    List<Person> searchByText(@Param("text") String text);

}
