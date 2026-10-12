# Nationality search intake and technical plan

This work item adds optional nationality data and a dedicated REST search while preserving existing API behaviour. Requirements and the technical plan are approved. Implementation and local verification are complete; independent review and PR CI remain pending.

## 1. Identity and traceability

| Field | Value |
| --- | --- |
| Jira key | AIDM-182 |
| Jira URL | https://arhs-group.atlassian.net/browse/AIDM-182 |
| Parent epic | AIDM-181, confirmed from Jira |
| Intake date | 2026-10-12 |
| Prepared by | Codex; requirements approved by Davide in this conversation |
| Source | Jira description retrieved by the approved read-only bridge through its temporary snapshot |
| Source fetched at (UTC) | 2026-10-12T09:10:58Z |
| Description SHA256 | ac9a6f6d8d5dbd90540d23c947ad6488c3806b31bd28757aee94107743684ae0 |

This is a paraphrased delivery record, not an export of the issue. Jira remains the requirements authority. Retain its BR and AC identifiers for traceability. Before implementation, verify a fresh snapshot and compare its description hash; reassess changed requirements rather than silently using this plan.

## 2. Outcome

Registry operators can retrieve people by a single nationality without searching through all registry records.

## 3. Approved requirements

All requirements are Must. REQ identifiers correspond one-to-one to the existing Jira BR identifiers.

| ID | Jira reference | Testable requirement |
| --- | --- | --- |
| REQ-001 | BR-001 | A person can carry one nationality value. |
| REQ-002 | BR-002 | Creation and replacement updates accept nationality. |
| REQ-003 | BR-003 | Person responses expose nationality. |
| REQ-004 | BR-004 | A dedicated REST operation retrieves people by nationality and rejects unusable search input. |
| REQ-005 | BR-005 | Search ignores differences in letter case. |
| REQ-006 | BR-006 | Search trims its input and compares the whole value, without substring matching. |
| REQ-007 | BR-007 | A valid search with no matches returns HTTP 200 and an empty JSON array. |
| REQ-008 | BR-008 | Existing endpoints and client payloads remain compatible. |
| REQ-009 | BR-009 | Nationality can be absent for both existing and new people. |

## 4. Scope boundaries

### In scope

- Nullable nationality storage, creation, update and JSON representation.
- `GET /person/nationality/{nationality}` and invalid-input handling.
- Domain, service, MVC, repository and full API lifecycle tests.
- Existing OpenAPI discovery of the new field and operation.

### Out of scope

- UI changes, authentication and authorization.
- Controlled vocabularies, ISO codes and multiple nationalities.
- Changes to general text search, other validations or age classification.
- Production-data migration; the configured database is in-memory H2.

Stored nationality retains the supplied spelling and case. Trimming applies to the search input; it does not introduce storage normalization.

## 5. Acceptance criteria

The AC identifiers retain Jira's original mapping.

| ID | Covers | Observable behaviour |
| --- | --- | --- |
| AC-001 | REQ-001, REQ-002, REQ-003 | Creating a valid person with nationality returns 201, exposes the value and persists it. |
| AC-002 | REQ-008, REQ-009 | Creation without nationality still returns 201; null serialization follows the existing configuration. |
| AC-003 | REQ-004, REQ-006 | Searching a complete nationality returns every matching person and excludes other nationalities. |
| AC-004 | REQ-005 | A search with different letter case returns the same matching people. |
| AC-005 | REQ-006 | Surrounding search whitespace does not change the result. |
| AC-006 | REQ-006 | A prefix-only search does not match a longer stored nationality. |
| AC-007 | REQ-007 | A valid nationality with no matches returns 200 and `[]`. |
| AC-008 | REQ-004 | An unusable search returns 400 with `{"message":"Nationality must not be blank"}`. |
| AC-009 | REQ-002, REQ-003 | Updating nationality returns 200; subsequent searches find the new value and exclude the previous value. |
| AC-010 | REQ-008 | Existing tests remain green, including clients that omit nationality. |

## 6. Questions and decisions

| Item | Owner | Status |
| --- | --- | --- |
| Jira description still labels itself DRAFT | Product owner | Requirements approved explicitly on 2026-10-12; no Jira edit made. |
| Embedded metadata says Related issue None | Codex | Use the actual Jira key and parent, not embedded metadata. |
| Codes, multiple nationalities, general search inclusion and mandatory nationality | Product owner | Deferred or excluded by Jira; do not expand scope. |
| Missing path value for AC-008 | Codex | Proposed plan: explicitly handle `/person/nationality` and `/person/nationality/` as invalid searches, as well as whitespace-only values. |

No unresolved Must requirement was identified. The missing-path handling above is an explicit proposed realization of Jira's unusable-input rule.

## 7. Technical plan

| Area | Planned change | Compatibility or risk |
| --- | --- | --- |
| Person | Add nullable String property with accessors; preserve the current constructor signature and copy nationality in the copy constructor. | Retain existing equality semantics: update duplicate checks currently depend on Person equality. Avoid unrelated changes to equality or logging. |
| Repository | Add an exact case-insensitive nationality finder returning a list. | Do not use LIKE or change text search. Repository tests must exclude null nationalities and verify multiple matches. |
| Service interface and implementation | Add nationality search; reject null/blank input, trim usable input and delegate to the repository. Copy nationality during update, including null. | PUT remains replacement-style, consistent with the current implementation; omission clears the optional value. Creation already saves the supplied entity. |
| Controller | Add the dedicated GET operation, return 200 for lists including empty ones, and translate invalid input to the specified message and 400. Handle missing path values explicitly. | Keep existing general-search 404 behaviour. Test routing so invalid requests do not fall into the numeric-ID handler. |
| OpenAPI | Verify the nullable field and new endpoint appear in generated API documentation. | Existing discovery should suffice; add annotations only if needed. |
| Persistence | Allow Hibernate to include the nullable field in the existing H2 schema lifecycle. | No new dependencies or migration framework. |

ADR: not required. This extends the existing Controller → Service → Repository architecture without a durable architectural change. Target Java 17 and Spring Boot 3.3.4.

## 8. Implementation record

| Item | Evidence |
| --- | --- |
| Branch | `codex/aidm-182-nationality-search` |
| Product components changed | Person, CivilRegistryRepository, CivilRegistryService, CivilRegistryServiceImpl, CivilRegistryController |
| Requirement and acceptance evidence | Domain, service, repository and API tests added; 80 local tests passed |
| Deviations | None |

Before implementation the snapshot was fresh (2026-10-12T09:19:04Z) and its description hash matched the approved version. The human authorized implementation explicitly in this conversation.

Local verification: the human reports `mvn --batch-mode --no-transfer-progress clean verify` successful on branch `codex/aidm-182-nationality-search`, implementation commit `32094a8`. Codex inspected the six Surefire reports: 80 tests, zero failures, zero errors and zero skipped tests; latest report timestamp 2026-10-12T09:35:31Z. Full Maven lifecycle success is human-reported; the test counts are directly verified from reports. Local Maven reports Java 21; PR CI must additionally verify Java 17. These verification-record changes are documentation-only.

Added test classes: `PersonTest`, `CivilRegistryRepositoryTest`, `NationalityApiIntegrationTest`. Extended `CivilRegistryServiceTest` and `CivilRegistryControllerTest`. No dependency changes were required.

The local Jira snapshot workaround is process infrastructure on a separate branch, not feature implementation evidence.

## 9. Test plan

| Acceptance criterion | Test level | Evidence expected |
| --- | --- | --- |
| AC-001, AC-002 | Domain, service and API integration | With/without nationality; response and persisted value; copy-constructor coverage. |
| AC-003, AC-004, AC-006, AC-007 | Repository integration with H2 | Exact case-insensitive query, multiple matches, exclusions, null values and empty results. |
| AC-004, AC-005, AC-007, AC-008 | Service unit | Trim before delegation; reject null/blank before repository invocation; preserve returned list. |
| AC-003 through AC-008 | MVC | Array responses and status codes; spaces, absent path value and trailing slash; exact error message. |
| AC-009 | Service and full API integration | Verify saved replacement nationality; create → search → update → search against real H2. Also cover clearing the optional value. |
| AC-010 | Regression | Entire existing suite; explicitly assert general-search empty results still return 404. |
| Documentation | API integration or recorded verification | Generated OpenAPI contains nationality and its dedicated operation. |

Existing MVC tests mock the service, so they cannot establish persistence or full lifecycle correctness. Add repository integration tests and a Spring Boot API integration test with an isolated in-memory database.

Local clean verify is reported successful and all 80 tests pass in the inspected reports. Require the GitHub Actions Quality gate on the reviewed PR commit. Performance remains a qualitative Jira expectation, not a fabricated numerical SLA.

## 10. Delivery checklist

- [x] Jira issue and parent epic verified using the bridge snapshot.
- [x] Requirements approved by the human and mapped to Jira identifiers.
- [x] Questions resolved or explicitly deferred; technical choices stated for review.
- [x] Human gives the implementation go-ahead for this plan.
- [x] Feature branch created; process infrastructure changes excluded from feature staging.
- [x] Implementation and tests cover acceptance criteria; independent assessment pending.
- [x] Local verification evidence recorded.
- [ ] GitHub Actions Quality gate green on the PR commit.
- [ ] Independent review recorded by a human or independent Codex session.
- [ ] Human approves merge.

Merge, release and Jira writes remain subject to explicit human authorization. At delivery, record intake, first implementation commit, PR and merge timestamps, test results, review findings, human interventions and available AI usage metrics.
