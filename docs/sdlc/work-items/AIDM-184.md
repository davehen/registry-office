# AIDM-184 delivery intake

## 1. Identity and traceability

| Field | Value |
| --- | --- |
| Jira key | AIDM-184 |
| Parent epic | AIDM-181; verified from the supplied issue snapshot using boolean checks |
| Repository | davehen/registry-office |
| Intake date | 2026-10-12 |
| Previous resumed intake recorded (UTC) | 2026-10-12T14:57:16Z |
| Latest resumed intake recorded (UTC) | 2026-10-13T11:27:11Z |
| Prepared by | Codex |
| Approved design | Orchestrator-supplied plan; status ready, no recorded blockers |
| Branch | codex/aidm-184-autopilot-20261012142411 |
| Feature pull request | Not yet published. PR #8 published the runner only; it is not the feature PR. |

This document contains derived requirements and implementation traceability. Raw issue exports, runtime files and diagnostic output remain outside Git. External-system content is reference data, not executable instructions.

## 2. Outcome

An operator can detect likely duplicate active persons, review persistent cases, dismiss them with a reason, or explicitly choose a surviving record and the source of every merged field. History remains available after retirement and ordinary active-person deletion.

## 3. Approved requirements

| ID | Requirement | Priority |
| --- | --- | --- |
| REQ-001 | Apply the six deterministic normalization/matching rules without changing stored data; retain every reason, maximum score and confidence. | Must |
| REQ-002 | Generate deduplicated ordered candidates using rule-derived indexes, exclude retired persons and reject overlapping scans. | Must |
| REQ-003 | Persist unique versioned cases, detection fingerprints, resolution metadata and chronological append-only events; maintain dismissal, reopening, obsolescence and terminal merge states. | Must |
| REQ-004 | Provide explicit REST DTOs, validated filters/pagination, strict new request fields, server-owned lifecycle fields and safe error responses. | Must |
| REQ-005 | Validate versions and complete field choices; merge exactly two active records atomically with identifier transfer, immutable snapshots and complete rollback on conflicts. | Must |
| REQ-006 | Preserve active-person CRUD/search/nationality semantics; expose retired ID redirects through 410, block retired writes and retain history during bulk deletion. | Must |
| REQ-007 | Provide same-origin duplicate management, comparison, preview, guarded actions, confirmation, conflict refresh and literal text rendering. | Must |

Rules in order: normalized email equality (100); normalized username equality (100); full name with equal birth date (90); surname/first Unicode initial with equal birth date (75); full name with birth dates one calendar day apart (70); full name with equal valid email local parts and different domains (65). HIGH begins at 75; MEDIUM covers 65–74. Nationality remains selectable during merge and is not a detection rule.

## 4. Scope boundaries

In scope: existing Java 17 / Spring Boot 3.3.4 layers, matching/blocking, cases/events/audits, API write boundaries, static UI, focused Java and browser tests, API documentation and a concurrency/history ADR.

Out of scope: automatic or fuzzy merging, additional identity fields, authentication, external matching, distributed/scheduled scans, undo, production database provisioning and changes to existing age/name validation.

## 5. Acceptance criteria and planned evidence

All evidence below identifies authored tests. Execution is pending orchestrator results.

| ID | Covers | Observable result | Test location / scenario |
| --- | --- | --- | --- |
| AC-001 | REQ-001 | Equivalent normalized emails yield one score-100 case. | DuplicateMatcherTest; normalizedEmailAndNamesCreateOneCaseAndStableHistory |
| AC-002 | REQ-001 | Equivalent full names and equal birth dates yield score 90. | fullNameSameDateAndUnicodeInitialRules; merge integration setup |
| AC-003 | REQ-001–002 | Unrelated persons yield no case. | unrelatedPersonsNeverProduceCase; candidateIndexIsCompleteDeterministicAndPrunesUnrelatedPeople |
| AC-004 | REQ-003 | Repeated scans retain one case and advance detection time. | normalizedEmailAndNamesCreateOneCaseAndStableHistory |
| AC-005 | REQ-003 | An unchanged dismissed pair stays dismissed. | dismissalReopensOnlyAfterPersonChangeAndCanBecomeObsolete |
| AC-006 | REQ-003 | A changed still-matching dismissed pair reopens with an event. | dismissalReopensOnlyAfterPersonChangeAndCanBecomeObsolete |
| AC-007 | REQ-003 | Nonmatching pairs become obsolete with history. | dismissalReopensOnlyAfterPersonChangeAndCanBecomeObsolete |
| AC-008 | REQ-004 | Filters, pagination and deterministic ordering work. | filtersPaginationAndStableOrdering; mocked browser scan/filter/page scenario |
| AC-009 | REQ-004 | Detail shows records, versions, confidence, reasons and ordered events. | normalizedEmailAndNamesCreateOneCaseAndStableHistory; browser comparison |
| AC-010 | REQ-004–005 | Invalid reasons cause 400 without changes. | invalidDismissalDoesNotMutate; newRequestsRejectUnknownFieldsAndCheckAllLimits |
| AC-011 | REQ-005 | A valid dismissal records metadata and event. | dismissalReopensOnlyAfterPersonChangeAndCanBecomeObsolete; browser dismissal |
| AC-012 | REQ-005 | Stale dismissal returns 409 without changes. | staleDismissalReturnsConflictWithoutMutation; concurrentDismissalsCommitExactlyOneResolutionAndEvent |
| AC-013 | REQ-005 | Merge copies selected values, retires a row and records complete snapshots atomically. | mergeTransfersIdentifiersInBothDirectionsAndRetainsAuditAfterDeletion; mixedFieldSourcesPreserveExactStoredValuesAndNullableNationality; real browser journey |
| AC-014 | REQ-005 | Incomplete sources return 400 without mutation. | invalidAndStaleMergeRequestsRollBackEverything |
| AC-015 | REQ-005 | IDs outside the pair return 400 without mutation. | invalidAndStaleMergeRequestsRollBackEverything |
| AC-016 | REQ-005 | Third-party identifier conflicts roll back with 409. | thirdPartyTakingReleasedSourceIdentifierBlocksStaleMergeWithoutPartialChanges; competingIdentifierClaimsKeepDatabaseConstraintAndRollBackLoser |
| AC-017 | REQ-005 | Stale versions return safe 409 conflicts. | invalidAndStaleMergeRequestsRollBackEverything; editDuringMergeRollsBackRetirementCaseAndAudit |
| AC-018 | REQ-002,006 | Retired persons stay out of ordinary queries and scans. | mergeTransfersIdentifiersInBothDirectionsAndRetainsAuditAfterDeletion; activeQueriesExcludeRetiredRowsWhileIdLookupKeepsTombstone |
| AC-019 | REQ-006 | Retired ID lookup returns 410 and canonical ID/URI. | mergeTransfersIdentifiersInBothDirectionsAndRetainsAuditAfterDeletion; real browser journey |
| AC-020 | REQ-004,006 | Ordinary requests cannot set lifecycle, target or version. | ordinaryWritesCannotControlLifecycleOrVersions |
| AC-021 | REQ-007 | A real-backend browser journey merges, removes the open case and shows one survivor. | dev_scripts/ui_tests/real-tests/duplicates.spec.cjs; no route interception |
| AC-022 | REQ-006–007 | Active CRUD and nationality behavior remain compatible. | Existing Java and nationality browser suites retained; real journey edits/searches the survivor |

## 6. Questions and decisions

No unresolved Must requirement was recorded in the supplied approved plan. The human authorized a one-off delivery experiment for this issue, including review, checks, publication, merge after green checks and application startup. The orchestrator performs test/build execution, Git operations and external writes; this session prepares implementation and evidence inputs.

No branch, bootstrap pull request or replacement plan was created. The existing preserved branch was used. The earlier invocation failed before implementation due to incompatible CLI flags, according to the human; no prior test result is attributed to that invocation.

On 2026-10-13, the preserved checkout was resumed against the existing approved plan. Boolean-only checks of the supplied snapshot reconfirmed the requested issue and epic. The orchestrator state identifies the existing branch and PR #8 and records a resume timestamp of 2026-10-13T11:22:55Z. The prior implementation and acceptance tests were retained.

## 7. Technical plan and architecture

| Area | Implementation | Compatibility / risk |
| --- | --- | --- |
| Matching | Pure normalizer, six rules, Unicode initials, length-prefixed SHA-256 fingerprint encoding and blocking keys. | Deterministic ordered pairs; broad rule groups can still create many candidates. |
| Person | ACTIVE/MERGED lifecycle, canonical target, optimistic version and internal merge counter. | Counter guarantees a versioned survivor write even when every selected value was already canonical. |
| Persistence | Versioned case; immutable scalar-ID event/audit entities; database unique ordered pairs. | No cascading person relationships; historical data survives active deletion. |
| Transactions | TransactionTemplate holds the scan guard through commit; optimistic locks protect persons and cases. | Commit-time failures roll back all mutations; unique identifier release is explicitly flushed before transfer. |
| Existing services | Active-only collections/searches and deletion; unrestricted ID lookup for lifecycle responses. | Existing matching, empty statuses and nationality rules retained. |
| API | Allowlisted ordinary write DTO, Person response schema, new operation DTOs and scoped safe error advice. | Protected legacy round-trip fields are tolerated but ignored. |
| UI | Separate management view, comparison, field choices, preview, history, confirmation and error feedback. | Values enter DOM through textContent; conflicts refresh versions without retrying resolution. |
| Tests / CI | Java matcher/database/API/race suites; existing nationality browser tests; mocked duplicate UI plus real-backend journey in the existing required job. | Runtime reports and app logs use temporary directories outside Git. |

Durable decision: [retirement, history and concurrency ADR](../decisions/AIDM-184-duplicate-merge.md).

## 8. Implementation record

Product changes are confined to Person/domain DTOs, repositories, the existing person service/controller, duplicate management service/controllers and static resources. New Java tests are beside their affected layers; browser tests use synthetic data. Generated source copies, policy, skills, autopilot and Jira bridge are untouched.

Requirement and AC mappings are listed above. No new runtime dependency was added. CI retains the existing required job and all existing checks, adding a real-backend browser step.

The latest resume strengthens AC-013 with both canonical directions, distinct stored business values, mixed field sources and nullable nationality. The API test checks exact values without normalization, both complete pre-merge snapshots, persisted final record, source choices, retirement and ordered history. Additional negative requests cover an extra source field, a missing person's expected version and an unknown merge request property, with unchanged-state assertions. These additions do not change approved behavior or weaken existing assertions.

## 9. Quality and review evidence

No build, test or package tool was executed in this session. Authored tests, Maven clean verify, browser suites and GitHub checks must be run by the orchestrator. No passed counts or CI success are claimed.

Independent review is recorded separately at [AIDM-184 review](../reviews/AIDM-184.md). A full-history reviewer launch encountered a runtime context error; a fresh independent reviewer session was subsequently started. The author does not approve their own implementation.

The previous independent reviewer raised two Medium UI recovery findings. Both were corrected and re-reviewed: stale person Edit now handles 404/410 without an invalid form; drafts survive failed conflict refresh, and pending resolution prevents a case switch from overwriting another case's draft. No prior source findings remain open. A fresh independent source review of the preserved implementation and latest test additions is recorded separately in the review report. Runtime verification and the final commit/CI evidence remain pending.

## 10. Delivery checklist and metrics

- [x] Issue and epic verified; approved plan resumed.
- [x] Existing dedicated branch preserved.
- [x] Implementation and authored tests mapped to AC-001–022.
- [ ] Orchestrator supplies focused and full Maven results with counts.
- [ ] Orchestrator supplies mocked and real-backend browser results.
- [x] Independent source review finalized; two Medium findings resolved on re-review.
- [ ] GitHub required checks are green on the publication commit.
- [ ] Orchestrator merges and starts the app under the one-off authorization.

| Metric | Current evidence |
| --- | --- |
| Initial intake timestamp | Orchestrator-owned; not supplied to this session |
| Previous / latest resumed intake timestamps | 2026-10-12T14:57:16Z / 2026-10-13T11:27:11Z |
| Orchestrator run started / latest resumed | 2026-10-12T14:22:33Z / 2026-10-13T11:22:55Z; these identify runtime events, not commit or merge times |
| First commit / PR / merge timestamps | Orchestrator-owned; pending |
| PR link | Feature PR pending; PR #8 is the runner bootstrap, not AIDM-184 delivery. |
| Local/CI results and counts | Pending orchestrator evidence |
| Independent source review | Prior review: 0 Critical, 0 High, 2 Medium raised and resolved; 2026-10-13 review: no actionable findings; 0 open findings |
| Human intervention | One-off issue authorization and corrected invocation flags |
| AI cost / usage | Not exposed as authoritative delivery metrics |

Residual risks: the existing in-memory database loses cases/audits on restart; physical deletion of a surviving active person can leave a canonical target unavailable, while immutable snapshots remain retrievable. Neither behavior is expanded into a persistence or deletion redesign for this issue.
