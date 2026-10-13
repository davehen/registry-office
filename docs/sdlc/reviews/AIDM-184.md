# Duplicate management independent review

## 1. Review identity

| Field | Value |
| --- | --- |
| Jira key | AIDM-184 |
| Pull request | Pending delivery coordinator |
| Commit reviewed | Uncommitted implementation snapshot; final commit identification pending coordinator |
| Branch | `codex/aidm-184-autopilot-20261012142411` (coordinator supplied) |
| Reviewer | Independent Codex agent `review_aidm184`, authorized by the human |
| Review date | 2026-10-12 |
| Requirements | Approved sanitized implementation plan and `docs/sdlc/work-items/AIDM-184.md`; requirement IDs below refer to that plan |

The reviewer inspected source and test files independently. Repository policy, the feature-delivery skill, and the sanitized approved plan were read. Raw Jira exports and credentials were not read. The coordinator owns Git operations, builds, test execution, and external writes. This report records source review, not successful execution or merge readiness.

## 2. Requirement coverage

| Requirement / acceptance criteria | Evidence inspected | Result |
| --- | --- | --- |
| REQ-001; AC-001–003 | `DuplicateMatcher`, normalization, all six reasons, maximum score, Unicode initials, email-local validation; `DuplicateMatcherTest` | No source finding; execution pending |
| REQ-002; AC-003–004, AC-018 | Rule-specific blocking keys, ordered deduplicated pairs, singleton guard held through transaction completion; candidate and overlap tests | No source finding; execution pending |
| REQ-003; AC-004–007, AC-009 | Versioned pair-unique cases, person/version fingerprints, immutable scalar-ID history, refresh/dismiss/reopen/obsolete transitions; lifecycle and persistence test sources | No source finding; execution pending |
| REQ-004; AC-008–012, AC-017, AC-020 | DTO allowlists, new-operation unknown-field rejection, bounded pagination, deterministic sorting/history, scoped safe error advice | No source finding; execution pending |
| REQ-005; AC-013–017 | Full selection validation before mutation, exact stored values, identifier-release flush before transfer, version checks, forced canonical versioned write, transactional audit/history; integration and competing-transaction test sources | No source finding; execution pending |
| REQ-006; AC-018–020, AC-022 | Active-only queries and deletion, unfiltered ID tombstones, 410 relative canonical URI, protected ordinary writes; Java regression and retention test sources | REV-001 resolved on re-review; execution pending |
| REQ-007; AC-021–022 | Relative fetches, textContent rendering, comparison/preview/confirmations, validation, pending resolution controls, mock-browser negative cases and actual-backend journey source | REV-001 and REV-002 resolved on re-review; execution pending |

The real-backend browser test does not intercept REST calls and checks the merge, survivor, retired lookup, audit, and subsequent nationality/edit/delete behavior. Its presence does not establish that AC-021 passed. Concurrency test source includes actual competing database transactions for dismissals, ordinary edits, merge/edit races, and identifier claims. Test counts and results are pending.

Final reconciliation also covered the strengthened create-copy assertion (server-owned zero input ID, original business-field equality and nationality), explicit stale merge case-version rejection, and survivor version advancement when all selected values are already canonical. The scoped error advice retains framework ErrorResponse HTTP status with a fixed safe message; regression test source checks 415 and 405 without assuming an application body for unmapped requests. No source defect was identified in these final changes.

The final accessibility adjustment gives Status, Confidence, and canonical-person selects explicit names consistent with their visible labels and browser selectors. `confidenceThresholdsAndScoreOrderingIncludeMediumCases` uses separate synthetic pairs to assert score 75 is HIGH, score 70 is MEDIUM, confidence filters return the corresponding pair, and descending-score listing returns 75 before 70. These final source changes introduce no open review finding; execution remains pending.

The sanitized intake, `docs/sdlc/decisions/AIDM-184-duplicate-merge.md`, `docs/duplicate-management.md`, and `docs/sdlc/delivery/AIDM-184.md` were reconciled with the implementation. They document the approved tombstone/history/concurrency design, API contracts, test handoff, authorization boundaries, and pending execution metrics without claiming passing verification.

## 3. Review checklist

- [x] Source inspected against approved requirements and technical plan.
- [x] Controller → Service → Repository and Java 17 / Spring Boot 3.3.4 conventions retained.
- [x] Matching does not mutate persisted identity values or log fingerprint inputs.
- [x] Merge audit snapshots and events survive ordinary person deletion by avoiding cascading relationships.
- [x] New duplicate UI renders record values, reasons, and operator names as literal text.
- [x] Relevant focused Java, concurrency, mocked browser, and real-backend browser test sources inspected.
- [x] Final API/error changes, strengthened assertions, intake, ADR, API documentation and PR-description input independently reconciled on source inspection.
- [x] Findings resolved in source and independently re-reviewed.
- [ ] Final change inventory and final commit independently reconciled by the delivery coordinator.
- [ ] Actual focused test results, counts, and `mvn clean verify` evidence supplied by the coordinator.
- [ ] GitHub Actions **Quality gate** green on the final PR head.

## 4. Findings

| ID | Severity | File / area | Finding | Required action |
| --- | --- | --- | --- | --- |
| REV-001 | Medium; resolved | `src/main/resources/static/app.js`, person Edit recovery; REQ-006/007 | Original finding: stale Edit parsed 410/404 error payloads as persons and opened an invalid form. Re-review confirms status is checked before parsing, merged/unavailable errors are distinguished, and the list refreshes without opening the form. | Resolved in source. Added browser cases inspect stale Edit receiving both 410 and 404; execution pending coordinator. |
| REV-002 | Medium; resolved | `src/main/resources/static/duplicates.js`, conflict refresh and case navigation; REQ-007 | Original failed-refresh draft loss is corrected with per-case drafts, retained editable controls, and explicit Refresh case. The related case-switch edge is also corrected: detail navigation is guarded while resolution is pending, existing and newly rendered Review buttons are disabled, and only the resolution's internal detail refresh bypasses the guard. | Resolved in source. Browser cases cover failed conflict-refresh recovery and delayed resolution with attempted second-case navigation, retained first-case drafts, and re-enabled navigation. Execution pending coordinator. |

No open source finding remains. Both Medium findings were corrected and independently re-reviewed. No Critical or High source finding was identified.

## 5. Decision

- **Recommendation:** Source review complete with no open findings; complete coordinator-owned verification and final evidence reconciliation before delivery.
- **Reasoning:** Backend matching, persistence, API boundaries, transactional resolution, and browser recovery align with the approved plan on source inspection. REV-001 and REV-002 are resolved. Java and browser test fixtures were source-checked, including delayed-response routing, recovery controls, and navigation guards; no apparent compile, syntax, or test API-use defect was identified. Source inspection does not establish build or test execution success.
- **Residual risks:** Final documentation, changed-file inventory, commit identity, test/build counts, and PR CI must be reconciled by the coordinator. In-memory database durability and deletion of historical current-record references remain documented approved limitations; immutable snapshots preserve merge evidence.

Only a human makes the final merge decision. This review does not authorize external writes, merge, or release.

## 6. Independent resumed source review — 2026-10-13

Reviewer: independent Codex agent `review_resume_source`, authorized for AIDM-184 in Epic AIDM-181. This is a separate conclusion from the earlier review. The reviewer read `AGENTS.md`, the repository feature-delivery skill, the sanitized approved plan and intake, then inspected the preserved source and acceptance-test files. The coordinator supplied the preserved branch and [PR #8](https://github.com/davehen/registry-office/pull/8); Git inventory and final commit reconciliation remain coordinator-owned.

| Scope / traceability | Actual source inspection | Conclusion |
| --- | --- | --- |
| REQ-001–002; AC-001–004, AC-018 | All six matcher rules, normalization, Unicode initial, valid local-part parsing, rule-derived blocking, deterministic ordered pairs, fingerprint encoding and transactional scan guard; matcher and overlap-test sources. | No actionable source finding; execution pending. |
| REQ-003–004; AC-004–012, AC-017, AC-020 | Versioned unique cases, lifecycle transitions, scalar immutable history, detail/list ordering, request allowlists, limits, DTOs and safe error responses; persistence/API test sources. | No actionable source finding; execution pending. |
| REQ-005; AC-013–017 | Complete field/version validation, exact-value selection, release-before-transfer flush, forced survivor version advancement, atomic retirement/audit/events and related-case obsolescence; integration and actual competing-transaction test sources. | No actionable source finding; execution pending. |
| REQ-006–007; AC-018–022 | Active-only CRUD/search queries, retired-ID responses and retained history; person/nationality regression sources; same-origin comparison/preview/confirmation, literal rendering, pending-action guards and conflict recovery; mocked and real-backend browser sources. | No actionable source finding; execution pending. |

The resumed AC-013 test `mixedFieldSourcesPreserveExactStoredValuesAndNullableNationality` was independently inspected after its addition. Both canonical directions use visibly different stored fields and mixed sources, including null and non-null nationality. It asserts the complete final DTO against selected raw values, persisted survivor equality, both pre-merge snapshots, source map, retained audit, retirement, case status and ordered events. Added negative cases cover an extra source-map field, a missing second person version and an unknown merge-request JSON property with unchanged case/person detail and no audit. These additions strengthen the acceptance evidence without changing product behavior.

The reviewer also inspected the existing controller/service/nationality test sources, static HTML/CSS/JavaScript, API documentation, concurrency/history ADR, delivery document, Maven configuration and Quality gate/browser configuration. The source keeps the required Java 17 / Spring Boot 3.3.4 layers, same-origin calls and literal value rendering. No executable instruction was taken from external content, and no raw Jira export or credential was read for this review.

**Findings:** 0 Critical, 0 High, 0 Medium, 0 Low actionable findings identified in this source inspection. The earlier UI recovery fixes were independently inspected in their current form; stale Edit checks status before opening a form, and failed conflict refresh/pending resolution preserve the correct case's text context.

**Recommendation:** Resumed source review complete with no open source findings. This reviewer ran no Git command, build, test or package tool and performed no external write. Focused/full Maven results and counts, mocked and real-browser results, final changed-file/commit reconciliation and required CI results remain pending orchestrator evidence. Source inspection does not establish passing verification or merge readiness. Existing in-memory durability and unavailable historical canonical targets after active deletion remain documented approved limitations.
