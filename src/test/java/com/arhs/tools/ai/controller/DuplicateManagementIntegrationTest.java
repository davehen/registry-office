package com.arhs.tools.ai.controller;

import com.arhs.tools.ai.domain.*;
import com.arhs.tools.ai.dto.DuplicateDtos.*;
import com.arhs.tools.ai.dto.PersonView;
import com.arhs.tools.ai.repository.*;
import com.arhs.tools.ai.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:duplicates-api;DB_CLOSE_DELAY=-1", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
class DuplicateManagementIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired CivilRegistryRepository persons;
    @Autowired DuplicateCaseRepository cases;
    @Autowired DuplicateCaseEventRepository events;
    @Autowired PersonMergeAuditRepository audits;
    @Autowired DuplicateManagementService service;
    @Autowired CivilRegistryService civil;

    @BeforeEach void clear() { events.deleteAll(); audits.deleteAll(); cases.deleteAll(); persons.deleteAll(); }
    private Person person(String username, String first, String last, String email, LocalDate date) {
        Person p = new Person(username, first, last, email, date); p.setNationality("French");
        return persons.saveAndFlush(p);
    }
    private List<Person> pair() {
        return List.of(person("one", "First", "Last", "one@example.test", LocalDate.of(1990, 1, 1)),
                person("two", "First", "Last", "two@example.test", LocalDate.of(1990, 1, 1)));
    }
    private CaseDetail scanPair() { service.scan(); return service.detail(cases.findAll().get(0).getId()); }
    private MergeRequest mergeRequest(CaseDetail d, int canonical, int source) {
        Map<String, Integer> sources = new HashMap<>(); DuplicateManagementService.FIELDS.forEach(field -> sources.put(field, source));
        return new MergeRequest(d.duplicateCase().version(), canonical,
                Map.of(d.leftPerson().id(), d.leftPerson().version(), d.rightPerson().id(), d.rightPerson().version()), sources, "Verified same individual", "Reviewer");
    }
    private org.springframework.test.web.servlet.ResultActions postJson(String path, Object body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)));
    }
    @Test void normalizedEmailAndNamesCreateOneCaseAndStableHistory() throws Exception {
        person("one", "José", "O’Neil", " Shared@EXAMPLE.test ", LocalDate.of(1990, 1, 1));
        person("two", "Jose", "o-neil", "shared@example.test", LocalDate.of(1990, 1, 1));
        postJson("/duplicate-cases/scan", Map.of()).andExpect(status().isOk()).andExpect(jsonPath("$.createdCases").value(1));
        CaseDetail d = service.detail(cases.findAll().get(0).getId());
        assertThat(d.duplicateCase().score()).isEqualTo(100);
        assertThat(d.duplicateCase().reasons()).containsExactly("EMAIL_EXACT", "FULL_NAME_BIRTH_DATE", "LAST_NAME_FIRST_INITIAL_BIRTH_DATE");
        service.scan();
        assertThat(cases.count()).isEqualTo(1); assertThat(events.count()).isEqualTo(1);
        assertThat(service.detail(d.duplicateCase().id()).duplicateCase().lastDetectedAt()).isAfterOrEqualTo(d.duplicateCase().lastDetectedAt());
        mvc.perform(get("/duplicate-cases/" + d.duplicateCase().id())).andExpect(status().isOk())
                .andExpect(jsonPath("$.leftPerson.version").exists()).andExpect(jsonPath("$.events[0].type").value("DETECTED"));
    }
    @Test void unrelatedPersonsNeverProduceCase() {
        person("one", "Alpha", "First", "one@example.test", LocalDate.of(1990, 1, 1));
        person("two", "Beta", "Second", "two@example.test", LocalDate.of(1980, 1, 1));
        assertThat(service.scan().evaluatedCandidatePairs()).isZero(); assertThat(cases.count()).isZero();
    }
    @Test void dismissalReopensOnlyAfterPersonChangeAndCanBecomeObsolete() throws Exception {
        List<Person> pair = pair(); CaseDetail d = scanPair();
        postJson("/duplicate-cases/" + d.duplicateCase().id() + "/dismiss",
                new DismissRequest(d.duplicateCase().version(), "  Confirmed different people  ", " Reviewer "))
                .andExpect(status().isOk()).andExpect(jsonPath("$.duplicateCase.status").value("DISMISSED"))
                .andExpect(jsonPath("$.duplicateCase.resolutionReason").value("Confirmed different people"));
        service.scan(); assertThat(service.detail(d.duplicateCase().id()).duplicateCase().status()).isEqualTo(DuplicateCase.Status.DISMISSED);
        assertThat(events.count()).isEqualTo(2);
        Person edit = persons.findById(pair.get(0).getId()).orElseThrow(); edit.setNationality("German"); civil.update(edit);
        service.scan();
        CaseDetail reopened = service.detail(d.duplicateCase().id());
        assertThat(reopened.duplicateCase().status()).isEqualTo(DuplicateCase.Status.OPEN);
        assertThat(reopened.duplicateCase().resolutionReason()).isNull();
        assertThat(reopened.events()).extracting(EventView::type).containsExactly("DETECTED", "DISMISSED", "REOPENED_AFTER_PERSON_CHANGE");
        edit = persons.findById(pair.get(0).getId()).orElseThrow(); edit.setFirstName("Different"); edit.setLastName("Unrelated"); civil.update(edit);
        service.scan(); assertThat(service.detail(d.duplicateCase().id()).duplicateCase().status()).isEqualTo(DuplicateCase.Status.OBSOLETE);
        edit = persons.findById(pair.get(0).getId()).orElseThrow(); edit.setFirstName("First"); edit.setLastName("Last"); civil.update(edit);
        service.scan(); assertThat(cases.count()).isEqualTo(1);
        assertThat(service.detail(d.duplicateCase().id()).duplicateCase().status()).isEqualTo(DuplicateCase.Status.OPEN);
    }
    @ParameterizedTest @ValueSource(strings = {"", "short", "         "})
    void invalidDismissalDoesNotMutate(String reason) throws Exception {
        pair(); CaseDetail d = scanPair(); long count = events.count();
        postJson("/duplicate-cases/" + d.duplicateCase().id() + "/dismiss", new DismissRequest(d.duplicateCase().version(), reason, null))
                .andExpect(status().isBadRequest());
        assertThat(events.count()).isEqualTo(count); assertThat(service.detail(d.duplicateCase().id()).duplicateCase()).isEqualTo(d.duplicateCase());
    }
    @Test void staleDismissalReturnsConflictWithoutMutation() throws Exception {
        pair(); CaseDetail d = scanPair(); service.scan(); CaseDetail current = service.detail(d.duplicateCase().id());
        postJson("/duplicate-cases/" + d.duplicateCase().id() + "/dismiss", new DismissRequest(d.duplicateCase().version(), "Verified different individuals", null))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.resource").value("duplicate-cases/" + d.duplicateCase().id()));
        assertThat(service.detail(d.duplicateCase().id()).duplicateCase()).isEqualTo(current.duplicateCase()); assertThat(events.count()).isEqualTo(1);
    }
    @Test void filtersPaginationAndStableOrdering() throws Exception {
        pair(); person("three", "First", "Last", "three@example.test", LocalDate.of(1990, 1, 1)); scanPair();
        var page = service.list(DuplicateCase.Status.OPEN, DuplicateCase.Confidence.HIGH, 0, 2);
        assertThat(page.totalElements()).isEqualTo(3); assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.content()).extracting(CaseView::id).isSorted();
        assertThat(service.list(DuplicateCase.Status.OPEN, DuplicateCase.Confidence.MEDIUM, 0, 20).content()).isEmpty();
        for (String query : List.of("page=-1", "size=0", "size=101", "status=invalid", "confidence=invalid", "page=no"))
            mvc.perform(get("/duplicate-cases?" + query)).andExpect(status().isBadRequest());
        mvc.perform(get("/duplicate-cases")).andExpect(status().isOk()).andExpect(jsonPath("$.size").value(20));
    }
    @Test void confidenceThresholdsAndScoreOrderingIncludeMediumCases() {
        person("one", "Alpha", "Surname", "one@example.test", LocalDate.of(1990, 1, 1));
        person("two", "Alpha", "Surname", "two@example.test", LocalDate.of(1990, 1, 2));
        person("three", "Beta", "Other", "three@example.test", LocalDate.of(1980, 1, 1));
        person("four", "Betty", "Other", "four@example.test", LocalDate.of(1980, 1, 1));
        service.scan();
        assertThat(service.list(DuplicateCase.Status.OPEN, null, 0, 20).content()).extracting(CaseView::score).containsExactly(75, 70);
        assertThat(service.list(DuplicateCase.Status.OPEN, DuplicateCase.Confidence.MEDIUM, 0, 20).content()).extracting(CaseView::score).containsExactly(70);
        assertThat(service.list(DuplicateCase.Status.OPEN, DuplicateCase.Confidence.HIGH, 0, 20).content()).extracting(CaseView::score).containsExactly(75);
    }
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void mergeTransfersIdentifiersInBothDirectionsAndRetainsAuditAfterDeletion(boolean canonicalLeft) throws Exception {
        List<Person> pair = pair(); CaseDetail d = scanPair();
        assertThat(d.duplicateCase().score()).isEqualTo(90);
        int canonical = pair.get(canonicalLeft ? 0 : 1).getId(), retiring = pair.get(canonicalLeft ? 1 : 0).getId();
        MergeRequest request = mergeRequest(d, canonical, retiring);
        String result = postJson("/duplicate-cases/" + d.duplicateCase().id() + "/merge", request).andExpect(status().isOk())
                .andExpect(jsonPath("$.evidence.canonicalAfter.userName").value(canonicalLeft ? "two" : "one"))
                .andReturn().getResponse().getContentAsString();
        AuditView audit = mapper.readValue(result, AuditView.class);
        assertThat(audit.evidence().leftBefore()).isEqualTo(d.leftPerson()); assertThat(audit.evidence().rightBefore()).isEqualTo(d.rightPerson());
        assertThat(audit.evidence().fieldSources()).isEqualTo(request.fieldSources());
        Person survivor = persons.findById(canonical).orElseThrow(), retired = persons.findById(retiring).orElseThrow();
        assertThat(survivor.getVersion()).isEqualTo(audit.evidence().canonicalAfter().version()).isGreaterThan(0);
        assertThat(retired.getStatus()).isEqualTo(Person.Status.MERGED); assertThat(retired.getUserName()).isNull(); assertThat(retired.getEmail()).isNull();
        assertThat(civil.findAll()).extracting(Person::getId).containsExactly(canonical);
        assertThat(civil.findByNationality("French")).extracting(Person::getId).containsExactly(canonical);
        assertThat(civil.search("First")).extracting(Person::getId).containsExactly(canonical);
        assertThat(service.scan().activePersons()).isEqualTo(1);
        mvc.perform(get("/person/" + retiring)).andExpect(status().isGone())
                .andExpect(jsonPath("$.canonicalPersonId").value(canonical)).andExpect(jsonPath("$.canonicalUri").value("/person/" + canonical));
        mvc.perform(delete("/person/" + retiring)).andExpect(status().isConflict());
        postJsonPutRetired(retired);
        mvc.perform(delete("/person")).andExpect(status().isOk());
        assertThat(persons.findById(retiring)).isPresent(); assertThat(persons.findById(canonical)).isEmpty();
        assertThat(service.audit(audit.id())).isEqualTo(audit); assertThat(events.count()).isEqualTo(2);
        mvc.perform(get("/person-merges/" + audit.id())).andExpect(status().isOk());
    }
    private void postJsonPutRetired(Person p) throws Exception {
        mvc.perform(put("/person").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(p))).andExpect(status().isConflict());
    }
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void mixedFieldSourcesPreserveExactStoredValuesAndNullableNationality(boolean canonicalLeft) throws Exception {
        Person left = person(" Left.User ", " Left First ", "Left-Surname", " Shared@EXAMPLE.test ", LocalDate.of(1985, 2, 3));
        left.setNationality(" French "); persons.saveAndFlush(left);
        Person right = person("Right.User", "Right First", "Right’Surname", "shared@example.test", LocalDate.of(1993, 4, 5));
        right.setNationality(null); persons.saveAndFlush(right);
        CaseDetail d = scanPair();
        assertThat(d.duplicateCase().reasons()).containsExactly("EMAIL_EXACT");
        PersonView canonical = canonicalLeft ? d.leftPerson() : d.rightPerson();
        PersonView retiring = canonicalLeft ? d.rightPerson() : d.leftPerson();
        Map<String, Integer> sources = Map.of("userName", retiring.id(), "firstName", canonical.id(),
                "lastName", retiring.id(), "email", canonical.id(), "birthDate", retiring.id(), "nationality", retiring.id());
        MergeRequest request = new MergeRequest(d.duplicateCase().version(), canonical.id(),
                Map.of(d.leftPerson().id(), d.leftPerson().version(), d.rightPerson().id(), d.rightPerson().version()),
                sources, "Confirmed mixed field selections", "Reviewer");
        String result = postJson("/duplicate-cases/" + d.duplicateCase().id() + "/merge", request)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        AuditView audit = mapper.readValue(result, AuditView.class);
        PersonView after = audit.evidence().canonicalAfter();
        assertThat(after).isEqualTo(new PersonView(canonical.id(), retiring.userName(), canonical.firstName(),
                retiring.lastName(), canonical.email(), retiring.birthDate(), retiring.nationality(),
                Person.Status.ACTIVE, null, after.version()));
        assertThat(after.version()).isGreaterThan(canonical.version());
        assertThat(PersonView.of(persons.findById(canonical.id()).orElseThrow())).isEqualTo(after);
        assertThat(audit.evidence().leftBefore()).isEqualTo(d.leftPerson());
        assertThat(audit.evidence().rightBefore()).isEqualTo(d.rightPerson());
        assertThat(audit.evidence().fieldSources()).isEqualTo(sources);
        assertThat(service.audit(audit.id())).isEqualTo(audit);
        assertThat(persons.findById(retiring.id()).orElseThrow().getStatus()).isEqualTo(Person.Status.MERGED);
        assertThat(civil.findAll()).extracting(Person::getId).containsExactly(canonical.id());
        assertThat(service.detail(d.duplicateCase().id()).duplicateCase().status()).isEqualTo(DuplicateCase.Status.MERGED);
        assertThat(events.findByCaseIdOrderByOccurredAtAscIdAsc(d.duplicateCase().id()))
                .extracting(DuplicateCaseEvent::getType).containsExactly("DETECTED", "MERGED");
    }
    @Test void relatedOpenCasesBecomeObsoleteWhenPersonRetires() {
        pair(); person("third", "First", "Last", "third@example.test", LocalDate.of(1990, 1, 1));
        CaseDetail d = scanPair();
        var audit = service.merge(d.duplicateCase().id(), mergeRequest(d, d.leftPerson().id(), d.leftPerson().id()));
        assertThat(audit.evidence().canonicalAfter().version()).isGreaterThan(d.leftPerson().version());
        assertThat(cases.findAll().stream().filter(c -> c.getStatus() == DuplicateCase.Status.OBSOLETE).toList()).hasSize(1);
        assertThat(cases.findAll().stream().filter(c -> c.getStatus() == DuplicateCase.Status.OBSOLETE).findFirst().orElseThrow().getResolutionReason())
                .isEqualTo("PERSON_MERGED_ELSEWHERE");
    }
    @Test void invalidAndStaleMergeRequestsRollBackEverything() throws Exception {
        pair(); CaseDetail d = scanPair(); MergeRequest valid = mergeRequest(d, d.leftPerson().id(), d.rightPerson().id());
        Map<String, Integer> incomplete = new HashMap<>(valid.fieldSources()); incomplete.remove("nationality");
        Map<String, Integer> outside = new HashMap<>(valid.fieldSources()); outside.put("email", 999999);
        Map<String, Integer> extra = new HashMap<>(valid.fieldSources()); extra.put("status", d.leftPerson().id());
        List<MergeRequest> invalid = List.of(
                new MergeRequest(valid.expectedCaseVersion(), valid.canonicalPersonId(), valid.expectedPersonVersions(), incomplete, valid.reason(), null),
                new MergeRequest(valid.expectedCaseVersion(), valid.canonicalPersonId(), valid.expectedPersonVersions(), extra, valid.reason(), null),
                new MergeRequest(valid.expectedCaseVersion(), valid.canonicalPersonId(), Map.of(d.leftPerson().id(), d.leftPerson().version()), valid.fieldSources(), valid.reason(), null),
                new MergeRequest(valid.expectedCaseVersion(), 999999, valid.expectedPersonVersions(), valid.fieldSources(), valid.reason(), null),
                new MergeRequest(valid.expectedCaseVersion(), valid.canonicalPersonId(), valid.expectedPersonVersions(), outside, valid.reason(), null));
        for (var request : invalid) postJson("/duplicate-cases/" + d.duplicateCase().id() + "/merge", request).andExpect(status().isBadRequest());
        Map<Integer, Long> stale = new HashMap<>(valid.expectedPersonVersions()); stale.put(d.leftPerson().id(), 100L);
        postJson("/duplicate-cases/" + d.duplicateCase().id() + "/merge", new MergeRequest(valid.expectedCaseVersion(), valid.canonicalPersonId(), stale, valid.fieldSources(), valid.reason(), null))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.resource").value("person/" + d.leftPerson().id()));
        postJson("/duplicate-cases/" + d.duplicateCase().id() + "/merge", new MergeRequest(valid.expectedCaseVersion() + 1, valid.canonicalPersonId(), valid.expectedPersonVersions(), valid.fieldSources(), valid.reason(), null))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.resource").value("duplicate-cases/" + d.duplicateCase().id()));
        assertThat(service.detail(d.duplicateCase().id())).isEqualTo(d); assertThat(audits.count()).isZero(); assertThat(events.count()).isEqualTo(1);
    }
    @Test void newRequestsRejectUnknownFieldsAndCheckAllLimits() throws Exception {
        pair(); CaseDetail d = scanPair(); String path = "/duplicate-cases/" + d.duplicateCase().id() + "/dismiss";
        postJson(path, Map.of("expectedCaseVersion", d.duplicateCase().version(), "reason", "Valid review reason", "surprise", "ignored?"))
                .andExpect(status().isBadRequest());
        var unknownMerge = mapper.valueToTree(mergeRequest(d, d.leftPerson().id(), d.rightPerson().id()));
        ((com.fasterxml.jackson.databind.node.ObjectNode) unknownMerge).put("surprise", "unexpected");
        postJson("/duplicate-cases/" + d.duplicateCase().id() + "/merge", unknownMerge).andExpect(status().isBadRequest());
        assertThat(service.detail(d.duplicateCase().id())).isEqualTo(d); assertThat(audits.count()).isZero();
        for (String operator : List.of(" ", "x".repeat(101))) postJson(path, new DismissRequest(d.duplicateCase().version(), "Valid review reason", operator)).andExpect(status().isBadRequest());
        postJson(path, new DismissRequest(d.duplicateCase().version(), "x".repeat(501), null)).andExpect(status().isBadRequest());
        postJson(path, new DismissRequest(null, "Valid review reason", null)).andExpect(status().isBadRequest());
        postJson(path, new DismissRequest(d.duplicateCase().version(), "x".repeat(10), "x".repeat(100))).andExpect(status().isOk());
    }
    @Test void ordinaryWritesCannotControlLifecycleOrVersions() throws Exception {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("userName", "new"); payload.put("firstName", "First"); payload.put("lastName", "Last"); payload.put("email", "new@example.test");
        payload.put("birthDate", "1990-01-01"); payload.put("status", "MERGED"); payload.put("mergedIntoPersonId", 99999); payload.put("version", 99999); payload.put("id", 99999);
        String response = postJson("/person", payload).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE")).andExpect(jsonPath("$.mergedIntoPersonId").isEmpty()).andExpect(jsonPath("$.version").value(0))
                .andReturn().getResponse().getContentAsString();
        int id = mapper.readTree(response).get("id").asInt(); assertThat(id).isNotEqualTo(99999);
        payload.put("id", id);
        mvc.perform(put("/person").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(payload)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE")).andExpect(jsonPath("$.mergedIntoPersonId").isEmpty());
    }
    @Test void missingCaseOrAuditReturnSafe404() throws Exception {
        mvc.perform(get("/duplicate-cases/999999")).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").exists());
        mvc.perform(get("/person-merges/999999")).andExpect(status().isNotFound());
    }

    @Test void frameworkRequestErrorsKeepTheirHttpStatusWithoutParserDetails() throws Exception {
        mvc.perform(post("/person").contentType(MediaType.TEXT_PLAIN).content("not-json"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(put("/duplicate-cases/scan"))
                .andExpect(status().isMethodNotAllowed());
    }
}
