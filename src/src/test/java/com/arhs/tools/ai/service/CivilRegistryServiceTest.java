package com.arhs.tools.ai.service;

import com.arhs.tools.ai.domain.Person;
import com.arhs.tools.ai.exception.DuplicatePropertyException;
import com.arhs.tools.ai.repository.CivilRegistryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CivilRegistryServiceTest {

    @Mock
    private CivilRegistryRepository repository;

    @InjectMocks
    private CivilRegistryServiceImpl service;

    private Person samplePerson;

    @BeforeEach
    void setUp() {
        samplePerson = new Person("testuser", "Test", "User", "test@example.com", LocalDate.now().minusYears(25));
        samplePerson.setId(1);
    }

    @Test
    void find_ExistingId_ShouldReturnPerson() {
        when(repository.findById(1)).thenReturn(Optional.of(samplePerson));
        Optional<Person> result = service.find(1);
        assertTrue(result.isPresent());
        assertEquals(samplePerson, result.get());
    }

    @Test
    void find_NonExistingId_ShouldReturnEmpty() {
        when(repository.findById(2)).thenReturn(Optional.empty());
        Optional<Person> result = service.find(2);
        assertTrue(result.isEmpty());
    }

    @Test
    void findByUserName_ExistingUser_ShouldReturnPerson() {
        when(repository.findByUserName("testuser")).thenReturn(Optional.of(samplePerson));
        Optional<Person> result = service.findByUserName("testuser");
        assertTrue(result.isPresent());
        assertEquals(samplePerson, result.get());
    }

    @Test
    void create_NewPerson_ShouldSaveAndReturnPerson() throws DuplicatePropertyException {
        when(repository.findByUserName(samplePerson.getUserName())).thenReturn(Optional.empty());
        when(repository.findByEmail(samplePerson.getEmail())).thenReturn(Optional.empty());
        when(repository.save(any(Person.class))).thenReturn(samplePerson);

        Person created = service.create(samplePerson);

        assertNotNull(created);
        verify(repository).save(samplePerson);
    }

    @Test
    void create_DuplicateUserName_ShouldThrowException() {
        when(repository.findByUserName(samplePerson.getUserName())).thenReturn(Optional.of(samplePerson));

        assertThrows(DuplicatePropertyException.class, () -> service.create(samplePerson));
        verify(repository, never()).save(any());
    }

    @Test
    void create_DuplicateEmail_ShouldThrowException() {
        when(repository.findByUserName(samplePerson.getUserName())).thenReturn(Optional.empty());
        when(repository.findByEmail(samplePerson.getEmail())).thenReturn(Optional.of(samplePerson));

        assertThrows(DuplicatePropertyException.class, () -> service.create(samplePerson));
        verify(repository, never()).save(any());
    }

    @Test
    void update_ExistingPerson_ShouldUpdateAndReturn() throws DuplicatePropertyException {
        Person updatedDetails = new Person("newuser", "New", "Name", "new@example.com", LocalDate.now().minusYears(20));
        updatedDetails.setId(1);

        when(repository.findById(1)).thenReturn(Optional.of(samplePerson));
        when(repository.findByUserName("newuser")).thenReturn(Optional.empty());
        when(repository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(repository.save(any(Person.class))).thenReturn(samplePerson);

        Optional<Person> result = service.update(updatedDetails);

        assertTrue(result.isPresent());
        assertEquals("newuser", samplePerson.getUserName());
        assertEquals("New", samplePerson.getFirstName());
        verify(repository).save(samplePerson);
    }

    @Test
    void update_NonExistingPerson_ShouldReturnEmpty() throws DuplicatePropertyException {
        when(repository.findById(anyInt())).thenReturn(Optional.empty());
        Optional<Person> result = service.update(samplePerson);
        assertTrue(result.isEmpty());
    }

    @Test
    void delete_ExistingId_ShouldDeleteAndReturnPerson() {
        when(repository.findById(1)).thenReturn(Optional.of(samplePerson));

        Optional<Person> result = service.delete(1);

        assertTrue(result.isPresent());
        verify(repository).delete(samplePerson);
    }

    @Test
    void delete_NonExistingId_ShouldReturnEmpty() {
        when(repository.findById(anyInt())).thenReturn(Optional.empty());
        Optional<Person> result = service.delete(2);
        assertTrue(result.isEmpty());
    }

    @Test
    void deleteByEmail_ExistingEmail_ShouldDeleteAndReturnPerson() {
        when(repository.findByEmail("test@example.com")).thenReturn(Optional.of(samplePerson));

        Optional<Person> result = service.deleteByEmail("test@example.com");

        assertTrue(result.isPresent());
        assertEquals(samplePerson, result.get());
        verify(repository).delete(samplePerson);
    }

    @Test
    void deleteByEmail_NonExistingEmail_ShouldReturnEmpty() {
        when(repository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        Optional<Person> result = service.deleteByEmail("nonexistent@example.com");

        assertTrue(result.isEmpty());
        verify(repository, never()).delete(any());
    }

    @Test
    void deleteAll_ShouldDeleteAllAndReturnList() {
        Iterable<Person> allPersons = List.of(samplePerson);
        when(repository.findAll()).thenReturn(allPersons);

        List<Person> result = service.deleteAll();

        assertEquals(1, result.size());
        verify(repository).deleteAll();
    }
}