package com.arhs.tools.ai.service;

import com.arhs.tools.ai.domain.*;
import com.arhs.tools.ai.dto.*;
import com.arhs.tools.ai.dto.DuplicateDtos.*;
import com.arhs.tools.ai.exception.RegistryConflictException;
import com.arhs.tools.ai.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class DuplicateManagementService {
    public static final Set<String> FIELDS = Set.of("userName", "firstName", "lastName", "email", "birthDate", "nationality");
    private final CivilRegistryRepository persons;
    private final DuplicateCaseRepository cases;
    private final DuplicateCaseEventRepository events;
    private final PersonMergeAuditRepository audits;
    private final DuplicateMatcher matcher;
    private final ObjectMapper mapper;
    private final EntityManager em;
    private final TransactionTemplate tx;
    private final AtomicBoolean scanning = new AtomicBoolean();

    public DuplicateManagementService(CivilRegistryRepository persons, DuplicateCaseRepository cases,
                                      DuplicateCaseEventRepository events, PersonMergeAuditRepository audits,
                                      DuplicateMatcher matcher, ObjectMapper mapper, EntityManager em,
                                      PlatformTransactionManager manager) {
        this.persons = persons; this.cases = cases; this.events = events; this.audits = audits;
        this.matcher = matcher; this.mapper = mapper; this.em = em; tx = new TransactionTemplate(manager);
    }
    public ScanResult scan() {
        if (!scanning.compareAndSet(false, true)) throw conflict("A duplicate scan is already running", "scan");
        // Hold the singleton guard through transaction completion, including rollback.
        try { return tx.execute(ignored -> performScan()); }
        finally { scanning.set(false); }
    }
    private ScanResult performScan() {
        List<Person> active = persons.findByStatus(Person.Status.ACTIVE);
        Map<Integer, Person> byId = new HashMap<>();
        active.forEach(p -> { byId.put(p.getId(), p); em.lock(p, LockModeType.OPTIMISTIC); });
        Map<DuplicateMatcher.Pair, DuplicateCase> existing = new HashMap<>();
        cases.findAll().forEach(c -> existing.put(new DuplicateMatcher.Pair(c.getLeftPersonId(), c.getRightPersonId()), c));
        var candidates = matcher.candidates(active);
        Set<DuplicateMatcher.Pair> matching = new HashSet<>();
        Instant now = Instant.now();
        int created = 0, reopened = 0, obsolete = 0;
        for (var pair : candidates) {
            Person left = byId.get(pair.left()), right = byId.get(pair.right());
            var match = matcher.match(left, right);
            if (match.score() == 0) continue;
            matching.add(pair);
            DuplicateCase c = existing.get(pair);
            String fingerprint = matcher.fingerprint(left, right);
            if (c == null) {
                c = new DuplicateCase(pair.left(), pair.right(), now);
                c.detect(match.score(), match.reasons(), fingerprint, now);
                cases.save(c); event(c, "DETECTED", now, null, null); created++;
            } else if (c.getStatus() != DuplicateCase.Status.MERGED) {
                boolean changed = !fingerprint.equals(c.getFingerprint());
                if (c.getStatus() == DuplicateCase.Status.OBSOLETE
                        || (c.getStatus() == DuplicateCase.Status.DISMISSED && changed)) {
                    c.reopen(); event(c, "REOPENED_AFTER_PERSON_CHANGE", now, null, null); reopened++;
                }
                c.detect(match.score(), match.reasons(), fingerprint, now);
            }
        }
        for (var entry : existing.entrySet()) {
            DuplicateCase c = entry.getValue();
            if (c.getStatus() != DuplicateCase.Status.MERGED && c.getStatus() != DuplicateCase.Status.OBSOLETE
                    && !matching.contains(entry.getKey())) {
                c.resolve(DuplicateCase.Status.OBSOLETE, "NO_LONGER_MATCHES", null, now, null);
                event(c, "NO_LONGER_MATCHES", now, null, null); obsolete++;
            }
        }
        cases.flush();
        return new ScanResult(active.size(), candidates.size(), matching.size(), created, reopened, obsolete);
    }
    public CasePage list(DuplicateCase.Status status, DuplicateCase.Confidence confidence, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Page must be nonnegative and size must be 1–100");
        return tx.execute(ignored -> {
            var result = cases.findAll((root, query, cb) -> confidence == null
                    ? cb.equal(root.get("status"), status)
                    : cb.and(cb.equal(root.get("status"), status), cb.equal(root.get("confidence"), confidence)),
                    PageRequest.of(page, size, Sort.by(Sort.Order.desc("score"), Sort.Order.asc("firstDetectedAt"), Sort.Order.asc("id"))));
            return new CasePage(result.map(CaseView::of).getContent(), page, size, result.getTotalElements(), result.getTotalPages());
        });
    }
    public CaseDetail detail(long id) { return tx.execute(ignored -> detailOf(caseById(id))); }
    private CaseDetail detailOf(DuplicateCase c) {
        return new CaseDetail(CaseView.of(c), persons.findById(c.getLeftPersonId()).map(PersonView::of).orElse(null),
                persons.findById(c.getRightPersonId()).map(PersonView::of).orElse(null),
                events.findByCaseIdOrderByOccurredAtAscIdAsc(c.getId()).stream().map(EventView::of).toList());
    }
    public CaseDetail dismiss(long id, DismissRequest request) {
        if (request == null) throw new IllegalArgumentException("Request is required");
        String reason = reason(request.reason()), operator = operator(request.operatorName());
        return tx.execute(ignored -> {
            DuplicateCase c = caseById(id); requireOpen(c, request.expectedCaseVersion());
            Instant now = Instant.now();
            c.resolve(DuplicateCase.Status.DISMISSED, reason, operator, now, null);
            event(c, "DISMISSED", now, reason, operator); cases.flush();
            return detailOf(c);
        });
    }
    public AuditView merge(long id, MergeRequest request) {
        if (request == null) throw new IllegalArgumentException("Request is required");
        String reason = reason(request.reason()), operator = operator(request.operatorName());
        return tx.execute(ignored -> {
            DuplicateCase c = caseById(id); requireOpen(c, request.expectedCaseVersion());
            Set<Integer> pair = Set.of(c.getLeftPersonId(), c.getRightPersonId());
            if (request.canonicalPersonId() == null || !pair.contains(request.canonicalPersonId()))
                throw new IllegalArgumentException("Canonical person must belong to the pair");
            if (request.fieldSources() == null || !request.fieldSources().keySet().equals(FIELDS)
                    || request.fieldSources().values().stream().anyMatch(source -> source == null || !pair.contains(source)))
                throw new IllegalArgumentException("Select one person from the pair for every mergeable field");
            if (request.expectedPersonVersions() == null || !request.expectedPersonVersions().keySet().equals(pair)
                    || request.expectedPersonVersions().values().stream().anyMatch(Objects::isNull))
                throw new IllegalArgumentException("Expected versions for both persons are required");
            Person left = activePerson(c.getLeftPersonId()), right = activePerson(c.getRightPersonId());
            for (Person p : List.of(left, right)) {
                if (p.getVersion() != request.expectedPersonVersions().get(p.getId()))
                    throw conflict("Person version has changed", "person/" + p.getId());
                em.lock(p, LockModeType.OPTIMISTIC);
            }
            PersonView leftBefore = PersonView.of(left), rightBefore = PersonView.of(right);
            Person canonical = left.getId() == request.canonicalPersonId() ? left : right;
            Person retired = canonical == left ? right : left;
            Map<String, Integer> sources = Map.copyOf(request.fieldSources());
            // Snapshot before clearing identifiers. Select only exact stored values.
            Person selected = new Person(canonical);
            selected.setUserName(source(sources, "userName", left, right).getUserName());
            selected.setFirstName(source(sources, "firstName", left, right).getFirstName());
            selected.setLastName(source(sources, "lastName", left, right).getLastName());
            selected.setEmail(source(sources, "email", left, right).getEmail());
            selected.setBirthDate(source(sources, "birthDate", left, right).getBirthDate());
            selected.setNationality(source(sources, "nationality", left, right).getNationality());
            checkUnique(selected, pair);
            retired.retireInto(canonical.getId());
            persons.flush(); // Free unique identifiers before the survivor takes them.
            canonical.setUserName(selected.getUserName()); canonical.setFirstName(selected.getFirstName());
            canonical.setLastName(selected.getLastName()); canonical.setEmail(selected.getEmail());
            canonical.setBirthDate(selected.getBirthDate()); canonical.setNationality(selected.getNationality());
            canonical.recordMerge(); persons.flush();
            Instant now = Instant.now();
            PersonMergeAudit audit = audits.save(new PersonMergeAudit(c.getId(), canonical.getId(), retired.getId(),
                    now, reason, operator, json(new MergeEvidence(leftBefore, rightBefore, PersonView.of(canonical), sources))));
            c.resolve(DuplicateCase.Status.MERGED, reason, operator, now, audit.getId());
            event(c, "MERGED", now, reason, operator);
            for (DuplicateCase related : cases.findAll()) {
                if (!related.getId().equals(c.getId()) && related.getStatus() == DuplicateCase.Status.OPEN
                        && (related.getLeftPersonId() == retired.getId() || related.getRightPersonId() == retired.getId())) {
                    related.resolve(DuplicateCase.Status.OBSOLETE, "PERSON_MERGED_ELSEWHERE", null, now, null);
                    event(related, "PERSON_MERGED_ELSEWHERE", now, null, null);
                }
            }
            cases.flush(); audits.flush();
            return auditOf(audit);
        });
    }
    private void checkUnique(Person selected, Set<Integer> pair) {
        if (selected.getUserName() != null && persons.findByUserName(selected.getUserName()).filter(p -> !pair.contains(p.getId())).isPresent())
            throw conflict("Selected username belongs to another person", "userName");
        if (selected.getEmail() != null && persons.findByEmail(selected.getEmail()).filter(p -> !pair.contains(p.getId())).isPresent())
            throw conflict("Selected email belongs to another person", "email");
    }
    private Person source(Map<String, Integer> sources, String field, Person left, Person right) {
        return sources.get(field) == left.getId() ? left : right;
    }
    public AuditView audit(long id) {
        return tx.execute(ignored -> auditOf(audits.findById(id).orElseThrow(() -> new NoSuchElementException("Merge audit not found"))));
    }
    private AuditView auditOf(PersonMergeAudit a) {
        try {
            return new AuditView(a.getId(), a.getCaseId(), a.getCanonicalPersonId(), a.getRetiredPersonId(),
                    a.getMergedAt(), a.getReason(), a.getOperatorName(), mapper.readValue(a.getEvidenceJson(), MergeEvidence.class));
        } catch (JsonProcessingException e) { throw new IllegalStateException("Audit evidence unavailable"); }
    }
    private String json(Object data) {
        try { return mapper.writeValueAsString(data); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Audit serialization unavailable"); }
    }
    private DuplicateCase caseById(long id) {
        return cases.findById(id).orElseThrow(() -> new NoSuchElementException("Duplicate case not found"));
    }
    private Person activePerson(int id) {
        Person p = persons.findById(id).orElseThrow(() -> conflict("Person is no longer available", "person/" + id));
        if (p.getStatus() != Person.Status.ACTIVE) throw conflict("Person has already been merged", "person/" + id);
        return p;
    }
    private void requireOpen(DuplicateCase c, Long expected) {
        if (expected == null || expected < 0) throw new IllegalArgumentException("Expected case version is required");
        if (c.getVersion() != expected) throw conflict("Case version has changed", "duplicate-cases/" + c.getId());
        if (c.getStatus() != DuplicateCase.Status.OPEN) throw conflict("Case is no longer open", "duplicate-cases/" + c.getId());
    }
    private void event(DuplicateCase c, String type, Instant at, String reason, String operator) {
        events.save(new DuplicateCaseEvent(c.getId(), type, at, reason, operator));
    }
    private String reason(String input) {
        String value = input == null ? "" : input.strip();
        if (value.length() < 10 || value.length() > 500) throw new IllegalArgumentException("Reason must contain 10–500 characters");
        return value;
    }
    private String operator(String input) {
        if (input == null) return null;
        String value = input.strip();
        if (value.isEmpty() || value.length() > 100) throw new IllegalArgumentException("Operator name must contain 1–100 characters");
        return value;
    }
    private RegistryConflictException conflict(String message, String resource) { return new RegistryConflictException(message, resource); }
}
