package com.arhs.tools.ai.controller;

import com.arhs.tools.ai.domain.Person;
import com.arhs.tools.ai.repository.CivilRegistryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:nationality-api;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class NationalityApiIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private CivilRegistryRepository repository;

    @BeforeEach
    void clearDatabase() {
        repository.deleteAll();
    }

    @Test
    void createSearchUpdateAndClearNationality() throws Exception {
        Person person = create("one", "French");
        assertThat(repository.findById(person.getId()).orElseThrow().getNationality()).isEqualTo("French");
        mvc.perform(get("/person/nationality/{nationality}", " French "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(person.getId()));

        person.setNationality("German");
        mvc.perform(put("/person").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(person)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nationality").value("German"));
        mvc.perform(get("/person/nationality/german"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(person.getId()));
        expectEmpty("French");

        person.setNationality(null);
        mvc.perform(put("/person").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(person)))
                .andExpect(status().isOk());
        assertThat(repository.findById(person.getId()).orElseThrow().getNationality()).isNull();
        expectEmpty("German");
    }

    @Test
    void exactSearchReturnsAllAndOnlyMatchingPeople() throws Exception {
        create("one", "French");
        create("two", "FRENCH");
        create("three", "German");
        create("four", null);
        mvc.perform(get("/person/nationality/french"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].userName", org.hamcrest.Matchers.containsInAnyOrder("one", "two")));
        expectEmpty("Fren");
        expectEmpty("Spanish");
    }

    @Test
    void oldPayloadWithoutNationalityStillWorks() throws Exception {
        Person person = new Person("legacy", "First", "Last", "legacy@example.test", LocalDate.of(1990, 1, 1));
        var payload = mapper.valueToTree(person);
        ((com.fasterxml.jackson.databind.node.ObjectNode) payload).remove("nationality");
        mvc.perform(post("/person").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(payload)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.userName").value("legacy"));
        assertThat(repository.findByUserName("legacy").orElseThrow().getNationality()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "   "})
    void whitespaceSearchReturnsSpecifiedError(String input) throws Exception {
        mvc.perform(get("/person/nationality/{nationality}", input))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Nationality must not be blank"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/person/nationality", "/person/nationality/"})
    void omittedNationalityReturnsSpecifiedError(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Nationality must not be blank"));
    }

    @Test
    void openApiDocumentsFieldAndEndpoint() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/person/nationality/{nationality}'].get").exists())
                .andExpect(jsonPath("$.components.schemas.Person.properties.nationality").exists());
    }

    private Person create(String username, String nationality) throws Exception {
        Person person = new Person(username, "First", "Last", username + "@example.test", LocalDate.of(1990, 1, 1));
        person.setNationality(nationality);
        String response = mvc.perform(post("/person").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(person)))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.nationality").value(nationality))
                .andReturn().getResponse().getContentAsString();
        return mapper.readValue(response, Person.class);
    }

    private void expectEmpty(String nationality) throws Exception {
        mvc.perform(get("/person/nationality/{nationality}", nationality))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }
}
