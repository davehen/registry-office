# Work-item intake

> Create a copy of this template for each Jira work item. Summarize the approved requirements; do not copy credentials, private attachments, or raw tool output into Git.

## 1. Identity and traceability

| Field | Value |
| --- | --- |
| Jira key | `<KEY>` |
| Jira URL | `<URL>` |
| Parent epic | `AIDM-181` |
| Intake date | `<YYYY-MM-DD>` |
| Prepared by | `Codex / human` |

## 2. Outcome

Describe the user or business outcome in one or two sentences.

## 3. Approved requirements

List the requirements in testable language. Assign stable IDs, for example `REQ-001`.

| ID | Requirement | Priority |
| --- | --- | --- |
| REQ-001 | `<testable behaviour>` | Must |

## 4. Scope boundaries

### In scope

- `<behaviour or component>`

### Out of scope

- `<explicit exclusion>`

## 5. Acceptance criteria

Express each criterion as an observable result and link it to one or more requirement IDs.

| ID | Covers | Given / When / Then |
| --- | --- | --- |
| AC-001 | REQ-001 | Given `<context>`, when `<action>`, then `<observable result>`. |

## 6. Open questions and decisions

| ID | Question or decision | Owner | Status |
| --- | --- | --- |
| Q-001 | `<question>` | Product owner | Open |

Do not start implementation while a Must requirement has an unresolved question.

## 7. Technical plan

Complete this section after the architecture gate.

| Area | Planned change | Compatibility or risk |
| --- | --- | --- |
| `<component>` | `<change>` | `<risk and mitigation>` |

State whether an ADR is needed. An ADR is required only for a durable architectural decision.

## 8. Implementation record

Complete this section while implementing the approved plan.

| Item | Evidence |
| --- | --- |
| Branch | `codex/<jira-key>-<short-description>` |
| Files or components changed | `<list>` |
| Requirement / acceptance-criterion traceability | `<REQ/AC IDs>` |
| Deliberate deviations from the plan | `<none, or explain and obtain approval>` |

Do not add behaviour outside the approved scope. Update the technical plan before implementing a material design change.

## 9. Test plan

| Acceptance criterion | Test level | Evidence expected |
| --- | --- | --- |
| AC-001 | Unit / service / API / end-to-end | `<test name or command>` |

The final quality gate is `mvn clean verify` plus the GitHub Actions **Quality gate** check.

## 10. Delivery checklist

- [ ] Jira item is in the pilot epic and requirements are complete.
- [ ] Open questions are resolved or explicitly deferred.
- [ ] Branch follows `codex/<jira-key>-<short-description>`.
- [ ] Implementation and tests trace back to acceptance criteria.
- [ ] Local verification evidence is recorded.
- [ ] GitHub Actions Quality gate is green on the pull request.
- [ ] Independent review is complete.
- [ ] Human approved the merge.
