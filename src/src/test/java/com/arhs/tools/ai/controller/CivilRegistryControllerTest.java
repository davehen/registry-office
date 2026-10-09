package com.arhs.tools.ai.controller;

import com.arhs.tools.ai.domain.Person;
import com.arhs.tools.ai.exception.DuplicatePropertyException;
import com.arhs.tools.ai.service.CivilRegistryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CivilRegistryController.class)
class CivilRegistryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CivilRegistryService service;

    @Autowired
    private ObjectMapper objectMapper;

    private Person samplePerson;

    @BeforeEach
    void setUp() {
        samplePerson = new Person("testuser", "Test", "User", "test@example.com", LocalDate.now().minusYears(25));
        samplePerson.setId(1);
    }

    @Test
    void getAll_ShouldReturnList() throws Exception {
        when(service.findAll()).thenReturn(List.of(samplePerson));

        mockMvc.perform(get("/person"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$[0].userName").value("testuser"));
    }

    @Test
    void getById_ExistingId_ShouldReturnPerson() throws Exception {
        when(service.find(1)).thenReturn(Optional.of(samplePerson));

        mockMvc.perform(get("/person/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("testuser"));
    }

    @Test
    void getById_NonExistingId_ShouldReturnNotFound() throws Exception {
        when(service.find(2)).thenReturn(Optional.empty());

        mockMvc.perform(get("/person/2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getByUserName_ExistingUser_ShouldReturnPerson() throws Exception {
        when(service.findByUserName("testuser")).thenReturn(Optional.of(samplePerson));

        mockMvc.perform(get("/person/username/testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userName").value("testuser"));
    }

    @Test
    void getByEmail_ExistingEmail_ShouldReturnPerson() throws Exception {
        when(service.findByEmail("test@example.com")).thenReturn(Optional.of(samplePerson));

        mockMvc.perform(get("/person/email/test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void search_ExistingResults_ShouldReturnList() throws Exception {
        when(service.search("test")).thenReturn(List.of(samplePerson));

        mockMvc.perform(get("/person/search/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userName").value("testuser"));
    }

    @Test
    void create_ValidPerson_ShouldReturnCreated() throws Exception {
        when(service.create(any(Person.class))).thenReturn(samplePerson);

        mockMvc.perform(post("/person")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(samplePerson)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void create_DuplicateProperty_ShouldReturnConflict() throws Exception {
        when(service.create(any(Person.class))).thenThrow(new DuplicatePropertyException(DuplicatePropertyException.UniqueProperty.USERNAME));

        mockMvc.perform(post("/person")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(samplePerson)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    void update_ExistingPerson_ShouldReturnOk() throws Exception {
        when(service.update(any(Person.class))).thenReturn(Optional.of(samplePerson));

        mockMvc.perform(put("/person")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(samplePerson)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void update_InvalidId_ShouldReturnBadRequest() throws Exception {
        Person invalidPerson = new Person("user", "First", "Last", "email@test.com", LocalDate.now());
        invalidPerson.setId(0);

        mockMvc.perform(put("/person")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidPerson)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A valid 'id' must be provided to update a person"));
    }

    @Test
    void deleteById_ExistingId_ShouldReturnOk() throws Exception {
        when(service.delete(1)).thenReturn(Optional.of(samplePerson));

        mockMvc.perform(delete("/person/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deleteById_NonExistingId_ShouldReturnNotFound() throws Exception {
        when(service.delete(2)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/person/2"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteAll_ShouldReturnList() throws Exception {
        when(service.deleteAll()).thenReturn(List.of(samplePerson));

        mockMvc.perform(delete("/person"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }
}