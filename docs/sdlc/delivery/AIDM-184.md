# AIDM-184 pull-request description input

## Work item

- Jira key: AIDM-184; Epic AIDM-181.
- Repository: davehen/registry-office.
- Intake: [approved requirement and acceptance traceability](../work-items/AIDM-184.md).
- Branch: `codex/aidm-184-autopilot-20261012142411`.
- Feature pull request: not yet published. PR #8 is the runner bootstrap, not this feature's delivery.

## Change summary

Operators can scan active records for deterministic duplicate matches, review persistent cases/history, dismiss with a reason, or explicitly select the surviving person and source of every merged field. Merge transfers unique identifiers transactionally, retains a retired record and immutable snapshots, and rolls back conflicting changes. Existing person and nationality flows use active records and handle retired IDs explicitly.

The static UI adds comparison, confidence/reasons, timestamps/versions, field preview, confirmation and conflict recovery. Reports and app logs remain in external temporary directories. No runtime dependency is added, and the existing required CI job retains its current checks and adds a real-backend browser journey.

## Acceptance-criteria evidence

Authored Java matcher, database, API and competing-transaction tests cover AC-001–020 and active-person regressions. Mocked browser cases cover validation, filters/pagination, literal rendering, status/network errors, failed refresh draft retention and pending resolution guards. A separate browser journey without intercepted requests covers AC-021 and post-merge nationality/edit/delete behavior for AC-022. Exact mappings are in the intake.

The resumed acceptance-test additions exercise both merge directions with mixed field sources and visibly distinct stored values, including nullable nationality. They check persisted exact values, complete audit snapshots and ordered events. Negative API cases additionally reject extra source fields, incomplete person-version maps and unknown merge request properties without mutation.

Local verification on commit `182777d060b07423801ba8a3a7d0cd00fb49c4c8` passed in the human's terminal on 2026-10-13: Maven clean verify, 130 Java tests, 31 mocked UI tests and 1 real-backend browser journey. Both browser reports confirm passed with zero failed tests. Subsequent delivery-evidence edits change documentation only; GitHub CI on the publication head remains required.

## Quality evidence

CI run `38397091972` failed on Java 17 with 130 tests, 3 failures and zero errors. All three failures compared the immediate merge audit to its persisted representation: nanosecond timestamps were rounded by timestamp(6) storage. The repair aligns audit and shared detection/resolution timestamps to microseconds before persistence, retaining exact audit-equality assertions. Two deterministic persistence regression cases cover nanoseconds and a second-boundary rounding case, forcing reload with EntityManager.clear(). Verification of this repair is pending; earlier passing evidence applies only to the older tested commit above.

- [x] Java: 130 passed, zero failures/errors/skips. Mocked UI: 31 passed. Real-backend browser: 1 passed.
- [x] `mvn clean verify` passed on the reviewed commit.
- [ ] GitHub Actions required Quality gate green on final PR head.
- [x] Source artifacts summarize approved behavior without raw Jira exports or credentials.
- [x] Independent source review completed and findings resolved.

## Independent review

- Reviewer: independent Codex agent `review_aidm184`.
- Resume reviewer: independent Codex agent `review_resume_source`, 2026-10-13; inspected the preserved implementation and additional acceptance-test sources with no actionable findings.
- Report: [AIDM-184 independent review](../reviews/AIDM-184.md).
- Findings: two Medium UI recovery findings raised, corrected and independently re-reviewed; no open source findings.
- Final independent reviewer: `aidm184_interactive_review`; approved source and independently inspected actual local test evidence on `182777d060b07423801ba8a3a7d0cd00fb49c4c8`, with no actionable findings. GitHub CI and publication remain pending.

## Delivery metrics

| Metric | Evidence |
| --- | --- |
| Previous / latest resumed intake recorded (UTC) | 2026-10-12T14:57:16Z / 2026-10-13T11:27:11Z |
| Orchestrator run started / latest resumed (UTC) | 2026-10-12T14:22:33Z / 2026-10-13T11:22:55Z |
| Initial intake / first commit / PR / merge timestamps | Pending orchestrator evidence |
| Local test result / totals | 130 Java, 31 mocked browser, 1 real-backend browser passed; clean verify succeeded |
| CI result | Pending final-head checks |
| Review findings | Prior review: 0 Critical, 0 High, 2 Medium resolved; 2026-10-13 source review: no actionable findings; 0 open source findings |
| Human interventions | Issue creation/authorization; multiple runner repairs and resume invocations; permission-profile approval; interactive continuation; terminal-only verification due runtime restrictions. Exact intervention count not recorded reliably. |
| AI runtime / cost | Authoritative metrics unavailable to this session |

## Merge and application handoff

The human's one-off authorization covers publication, merge after green required checks, and app startup for AIDM-184. The orchestrator performs Git mutations, external writes and execution. No check bypass is authorized. Jira status/comments are not part of this delivery.

Residual limitations: the existing in-memory database is transient across restart; ordinary deletion of an active survivor can leave a recorded canonical target unavailable, while snapshots remain retrievable. Distributed scans, undo and production persistence redesign remain outside scope.
