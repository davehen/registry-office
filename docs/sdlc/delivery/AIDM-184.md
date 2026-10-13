# AIDM-184 pull-request description input

## Work item

- Jira key: AIDM-184; Epic AIDM-181.
- Repository: davehen/registry-office.
- Intake: [approved requirement and acceptance traceability](../work-items/AIDM-184.md).
- Branch: `codex/aidm-184-autopilot-20261012142411`.
- Pull request: [preserved PR #8](https://github.com/davehen/registry-office/pull/8), identified by orchestrator state; final publication head and checks pending.

## Change summary

Operators can scan active records for deterministic duplicate matches, review persistent cases/history, dismiss with a reason, or explicitly select the surviving person and source of every merged field. Merge transfers unique identifiers transactionally, retains a retired record and immutable snapshots, and rolls back conflicting changes. Existing person and nationality flows use active records and handle retired IDs explicitly.

The static UI adds comparison, confidence/reasons, timestamps/versions, field preview, confirmation and conflict recovery. Reports and app logs remain in external temporary directories. No runtime dependency is added, and the existing required CI job retains its current checks and adds a real-backend browser journey.

## Acceptance-criteria evidence

Authored Java matcher, database, API and competing-transaction tests cover AC-001–020 and active-person regressions. Mocked browser cases cover validation, filters/pagination, literal rendering, status/network errors, failed refresh draft retention and pending resolution guards. A separate browser journey without intercepted requests covers AC-021 and post-merge nationality/edit/delete behavior for AC-022. Exact mappings are in the intake.

The resumed acceptance-test additions exercise both merge directions with mixed field sources and visibly distinct stored values, including nullable nationality. They check persisted exact values, complete audit snapshots and ordered events. Negative API cases additionally reject extra source fields, incomplete person-version maps and unknown merge request properties without mutation.

These are test sources, not passing execution evidence. The coordinator must replace pending results with actual command/CI evidence on the final commit.

## Quality evidence

- [ ] Relevant Java and mocked/real browser suites passed; counts pending orchestrator.
- [ ] `mvn clean verify` passed; result pending orchestrator.
- [ ] GitHub Actions required Quality gate green on final PR head.
- [x] Source artifacts summarize approved behavior without raw Jira exports or credentials.
- [x] Independent source review completed and findings resolved.

## Independent review

- Reviewer: independent Codex agent `review_aidm184`.
- Resume reviewer: independent Codex agent `review_resume_source`, 2026-10-13; inspected the preserved implementation and additional acceptance-test sources with no actionable findings.
- Report: [AIDM-184 independent review](../reviews/AIDM-184.md).
- Findings: two Medium UI recovery findings raised, corrected and independently re-reviewed; no open source findings.
- Runtime evidence and final commit reconciliation remain pending. Source review alone does not authorize merge readiness.

## Delivery metrics

| Metric | Evidence |
| --- | --- |
| Previous / latest resumed intake recorded (UTC) | 2026-10-12T14:57:16Z / 2026-10-13T11:27:11Z |
| Orchestrator run started / latest resumed (UTC) | 2026-10-12T14:22:33Z / 2026-10-13T11:22:55Z |
| Initial intake / first commit / PR / merge timestamps | Pending orchestrator evidence |
| Local test result / totals | Not executed in developer session; pending orchestrator |
| CI result | Pending final-head checks |
| Review findings | Prior review: 0 Critical, 0 High, 2 Medium resolved; 2026-10-13 source review: no actionable findings; 0 open source findings |
| Human interventions | One-off issue authorization; corrected prior invocation flags |
| AI runtime / cost | Authoritative metrics unavailable to this session |

## Merge and application handoff

The human's one-off authorization covers publication, merge after green required checks, and app startup for AIDM-184. The orchestrator performs Git mutations, external writes and execution. No check bypass is authorized. Jira status/comments are not part of this delivery.

Residual limitations: the existing in-memory database is transient across restart; ordinary deletion of an active survivor can leave a recorded canonical target unavailable, while snapshots remain retrievable. Distributed scans, undo and production persistence redesign remain outside scope.
