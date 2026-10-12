# Nationality user interface delivery

The web interface completes nationality support with creation, editing, display and combined search. Requirements and the technical plan were approved by the human. Implementation, browser verification and local Maven verification are complete; real-application smoke check, independent review and PR CI remain pending.

## 1. Identity and traceability

| Field | Value |
| --- | --- |
| Jira key | AIDM-183 |
| Jira URL | https://arhs-group.atlassian.net/browse/AIDM-183 |
| Parent epic | AIDM-181 |
| Intake date | 2026-10-12 |
| Prepared by | Codex; scope and implementation approved by Davide |
| Source | Jira description via the approved read-only bridge snapshot |
| Approved description SHA256 | 4940e1247c43c7cc42e5767b4b85e9a797454bebb10cff854f50d7bd5d9d46c1 |
| Pre-implementation source fetched at (UTC) | 2026-10-12T10:12:56Z |
| Dependency | AIDM-182 merged into main at 1ac4f81 |

This record summarizes the Jira requirements without storing a raw export or credentials. Jira remains the source of truth; future changes require a fresh snapshot and reassessment.

## 2. Outcome

An operator can manage nationality and find people using the existing web interface and its single search field.

## 3. Approved requirements

Preserve the Jira BR identifiers. REQ identifiers map one-to-one to them.

| ID | Jira | Requirement |
| --- | --- | --- |
| REQ-001 | BR-001 | An optional labelled nationality field appears in create/edit forms. |
| REQ-002 | BR-002 | Creation submits the entered value or null for an empty field. |
| REQ-003 | BR-003 | Editing loads the existing value and preserves it during unrelated edits. |
| REQ-004 | BR-004 | Editing can replace nationality or clear it to null. |
| REQ-005 | BR-005 | The list shows nationality or Not specified. |
| REQ-006 | BR-006 | Nationality is rendered literally, not as HTML. |
| REQ-007 | BR-007 | The existing search input queries both text fields and nationality. |
| REQ-008 | BR-008 | Text matching remains unchanged; nationality matching is exact, case-insensitive and trims input. |
| REQ-009 | BR-009 | Results are the union of both searches, deduplicated by person ID. |
| REQ-010 | BR-010 | No matches clears the list and displays No persons found. |
| REQ-011 | BR-011 | Empty and whitespace-only search restores the full list. |
| REQ-012 | BR-012 | Unexpected failures produce a visible error without successful-looking partial or stale results. |
| REQ-013 | BR-013 | Existing validation and person operations continue to work. |

## 4. Scope boundaries

In scope: existing form, list and search field; result union, error/empty states and browser tests. Use the existing REST endpoints without changing backend contracts.

Out of scope: a separate nationality search control, ISO validation, controlled vocabulary, multiple nationalities, mandatory nationality and altered text-matching rules.

## 5. Acceptance criteria

| ID | Covers | Observable result |
| --- | --- | --- |
| AC-001 | REQ-001, REQ-002, REQ-005 | Creating with nationality sends the value and displays the saved value. |
| AC-002 | REQ-002, REQ-005 | Creating with an empty field sends null and displays Not specified. |
| AC-003 | REQ-003 | Editing loads nationality; saving another field preserves it. |
| AC-004 | REQ-004 | Replacement and clearing persist and appear in the list. |
| AC-005 | REQ-001 | Saving or resetting prevents nationality carrying into a new person. |
| AC-006 | REQ-001, REQ-006 | Label and keyboard access work; markup-like data is literal text. |
| AC-007 | REQ-007, REQ-009 | Text-only and nationality-only matches both appear. |
| AC-008 | REQ-009 | A person matching both appears once by ID. |
| AC-009 | REQ-008 | Case and surrounding spaces work for nationality; a prefix alone is not a nationality match, while partial text matches remain valid. |
| AC-010 | REQ-007, REQ-009 | Text-search 404 or nationality-search empty array still permits the other successful matches. |
| AC-011 | REQ-010 | Neither search matching clears previous rows and shows the empty message. |
| AC-012 | REQ-011 | Empty or whitespace-only input shows all people. |
| AC-013 | REQ-012 | Either HTTP or network failure displays an error without partial or old results. |
| AC-014 | REQ-013 | Automated UI tests cover the new behaviour and existing creation, editing, deletion and search. |

## 6. Questions and decisions

No unresolved Must requirement was identified. The human explicitly approved integrating nationality into the existing search field and the updated Jira-based technical plan. The earlier UI-only scope has been superseded by Jira's new description.

- Empty means an empty form value; nonempty stored values keep their spelling and case.
- A text search passes the original input to preserve its matching behaviour; the nationality query trims its input.
- Treat only text-search 404 as an expected no-match condition. Nationality-search errors remain failures.
- Display text matches followed by nationality-only matches; no additional sorting requirement exists.

## 7. Technical plan

| Area | Implementation | Risk or compatibility |
| --- | --- | --- |
| index.html | Optional nationality input, associated label, table column, accessible search status and search input name. | Existing required-field validation retained. |
| app.js form | Include value/null in requests, populate during edit, use existing form reset lifecycle. | Saving unrelated edits preserves existing nationality. |
| app.js list | Create cells with textContent and buttons with event listeners. | Removes HTML interpolation in the affected renderer; existing actions remain available. |
| app.js search | Request existing text and nationality endpoints, normalize expected empty responses, validate arrays, union by ID. | No endpoint or backend contract changes. |
| Async list state | Clear stale rows while loading; only the latest request may update the table or message. | Delayed search/load responses cannot overwrite a newer outcome. |
| style.css | Allow modal scrolling. | The additional field remains reachable on shorter screens. |
| Browser tests | Isolated Playwright tests using real UI files and controlled API responses. | They verify UI/request contracts, not real backend persistence; AIDM-182 Java integration tests cover backend behaviour. |
| CI | Add Node 24, pinned pnpm/Playwright, Chromium installation and browser tests to the existing required job. | The existing Java 17 check cannot pass without the browser steps succeeding; job name remains unchanged. |

No architectural ADR is needed: this extends the existing static frontend using its current REST API. Playwright is a development-only dependency in dev_scripts/ui_tests; the production Java application gains no dependency.

## 8. Implementation record

| Item | Evidence |
| --- | --- |
| Branch | codex/aidm-183-nationality-ui |
| Product files | src/main/resources/static/index.html, app.js, style.css |
| Test infrastructure | dev_scripts/ui_tests and .github/workflows/quality-gate.yml |
| Traceability | REQ/BR and AC mapping above; tests below |
| Deviations | None; handling response ordering and modal scrolling supports approved usable UI behaviour. |

The local Jira workaround changes are excluded from feature commits.

## 9. Test plan and evidence

Browser suite: dev_scripts/ui_tests/tests/nationality.spec.cjs. Scenarios cover every AC, including null payloads, edit preservation/change/clear, reset, accessible field, literal markup, union/deduplication, case/whitespace/partial matching, one/both searches empty, show-all input, both endpoints failing via HTTP/network/malformed data, delayed-response ordering and operation regressions.

Local static verification passed: JavaScript syntax checks, git diff --check, package/lock version consistency and dependency integrity matching the provided runtime's lockfile. The lockfile was assembled from those recorded resolutions without executing a package installer; actual frozen installation still requires verification.

Browser verification passed in the human's terminal on commit `6ff565b`: all 17 scenarios are reported passing, and Codex directly inspected Playwright's `.last-run.json` with `status: passed` and an empty failedTests list. Successful dependency/browser setup is evidenced by the completed test execution. The human also reports successful local Maven clean verify on this feature branch; Codex inspected six Surefire reports with 80 tests, zero failures/errors/skips, latest report 2026-10-12T13:32:17Z, and confirmed the packaged JAR exists. Full Maven lifecycle success remains human-reported. Real-application smoke check and PR CI remain pending. Automatic approval review rejected running the cached Playwright dependency from Codex; the human or CI executes verification:

```bash
mvn --batch-mode --no-transfer-progress clean verify
cd dev_scripts/ui_tests
pnpm install --frozen-lockfile --ignore-scripts
pnpm exec playwright install chromium
pnpm test
```

Use Node 24 and pnpm 11.25.0. Chromium tests use only the local static test server and synthetic API fixtures. Also perform a manual smoke check of the running Java application to confirm the visible UI and actual REST integration.

First human browser execution: 16 of 17 tests passed. The keyboard-access test assumed a single Tab exits a native date input; that assumption is browser-dependent. The corrected test navigates backward from the adjacent Save button and types using the keyboard. The human reran the suite successfully, with the local report confirming no failures. PR CI is not yet verified.

## 10. Delivery checklist

- [x] Fresh Jira source and parent epic verified; AIDM-182 dependency merged.
- [x] Expanded scope and technical plan approved by the human.
- [x] Dedicated feature branch and scoped implementation prepared.
- [x] Browser tests map to acceptance criteria.
- [x] Browser tests pass after dependency/browser setup; 17 scenarios reported passing, local success report inspected.
- [x] Maven clean verify reported passing on this feature branch; 80 passing Java tests directly verified in reports.
- [ ] Manual UI smoke check against the real application recorded.
- [ ] Independent review complete.
- [ ] GitHub Java 17 and UI quality gate passes on PR head.
- [ ] Human approves merge.

At delivery record test counts, reviewed commit, findings, intake/commit/PR/merge times, human interventions and available usage metrics. Push, PR creation, Jira changes, merge and release require human authorization.
