# Duplicate management

Use **Duplicate Management** in the web application to scan active people, filter cases by status/confidence, compare records and review event history. Scans never merge automatically. A dismissal needs a 10–500 character reason and confirmation. A merge additionally needs a canonical person and a source person for all six fields, including nationality; check the final preview and confirm the irreversible action. Conflict feedback refreshes the case and requires a new review. Optional operator names contain 1–100 characters when supplied.

## API

All URLs are relative to the same application origin. Generated OpenAPI is available at `/v3/api-docs`.

| Method / path | Behavior |
| --- | --- |
| POST /duplicate-cases/scan | Scan active persons; return active-person, candidate, match and lifecycle counts. Overlap returns 409. |
| GET /duplicate-cases | Filters: status (default OPEN), optional confidence; page (default 0), size (default 20, maximum 100). Returns content and pagination metadata. |
| GET /duplicate-cases/{id} | Case metadata, current records (null when deleted), versions and chronological events. |
| POST /duplicate-cases/{id}/dismiss | Body: expectedCaseVersion, reason, optional operatorName. |
| POST /duplicate-cases/{id}/merge | Body: expectedCaseVersion, canonicalPersonId, expectedPersonVersions keyed by both IDs, fieldSources keyed by all six business fields, reason, optional operatorName. |
| GET /person-merges/{id} | Immutable pre-merge snapshots, final snapshot, field sources and merge metadata. |

Field-source keys are `userName`, `firstName`, `lastName`, `email`, `birthDate` and `nationality`. Each value is one of the case's two IDs. New resolution requests reject unknown properties. Ordinary person requests accept legacy round-trip properties while applying only business fields; ID is used only to identify an update. Lifecycle, target and version remain server-owned.

Lists/searches return active persons. A retired ID returns 410 with `canonicalPersonId` and a relative `canonicalUri`; updates/deletes return 409. A 400 indicates invalid input, 404 unavailable case/audit, and 409 a stale version, identifier conflict, overlapping scan or unavailable merge record. Error responses do not disclose database or parser details.

## Verification handoff

The orchestrator runs focused Java suites and `mvn --batch-mode --no-transfer-progress clean verify`, then the existing browser suite and `pnpm exec playwright test --config playwright.real.config.cjs`. For the real suite it starts the packaged application and supplies `REGISTRY_BASE_URL`. Tests use synthetic records and do not intercept requests in the real suite. Configure `REGISTRY_UI_RESULTS` and `REGISTRY_REAL_UI_RESULTS` to directories outside the checkout, or use the default system temporary directories. Keep application logs and all runtime artifacts outside Git.

The existing GitHub Quality gate retains its Java and browser checks and adds the real application journey. Execution results, independent review and green required checks are delivery prerequisites. Authoring these tests does not establish that they passed.
