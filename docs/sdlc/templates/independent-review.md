# Independent review report

> Complete this report after implementation and before the human merge decision. The reviewer must not approve a change they authored. Review the pull-request diff and the evidence; do not trust an implementation summary without checking it.

## 1. Review identity

| Field | Value |
| --- | --- |
| Jira key | `<KEY>` |
| Pull request | `<URL or number>` |
| Commit reviewed | `<SHA>` |
| Reviewer | `<human or independent Codex session>` |
| Review date | `<YYYY-MM-DD>` |

## 2. Requirement coverage

| Requirement / acceptance criterion | Evidence reviewed | Result |
| --- | --- | --- |
| `<REQ or AC ID>` | `<test, diff location, or CI result>` | Pass / Fail / Not applicable |

## 3. Review checklist

- [ ] The change is within the approved scope; no unapproved behaviour was added.
- [ ] The implementation follows the approved technical plan, or deviations are documented and approved.
- [ ] Existing API and behaviour remain compatible where required.
- [ ] Error handling and input validation are clear and consistent with the application.
- [ ] Tests cover the relevant acceptance criteria and meaningful negative cases.
- [ ] `mvn clean verify` and the GitHub Actions **Quality gate** are green.
- [ ] No secrets, credentials, private Jira content, or generated build artefacts were added.
- [ ] Dependencies and permissions did not broaden unexpectedly.

## 4. Findings

| ID | Severity | File / area | Finding | Required action |
| --- | --- | --- | --- | --- |
| REV-001 | Critical / High / Medium / Low | `<path>` | `<observation>` | `<fix or rationale>` |

Use `None` if there are no findings. Critical and High findings block merge. Medium findings require an explicit human decision. Low findings may be deferred with a recorded rationale.

## 5. Decision

- **Recommendation:** Approve / Request changes / Escalate
- **Reasoning:** `<concise evidence-based summary>`
- **Residual risks or follow-ups:** `<none, or list>`

Only a human makes the final merge decision.
