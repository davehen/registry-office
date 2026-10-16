## Work item

- Jira key: AIDM-185, pilot Epic AIDM-181.
- Jira URL: https://arhs-group.atlassian.net/browse/AIDM-185
- Intake artifact: docs/sdlc/work-items/AIDM-185.md
- Branch: codex/aidm-185-decouple-frontend-backend

## Change summary

Separate the REST backend and static frontend into independently buildable Maven modules, preserving current registry, nationality and duplicate-management capabilities. The backend no longer serves application UI assets; the Vite frontend uses an environment-configured REST client and exact-origin backend CORS.

Add a pinned local Node/npm toolchain, Maven Wrapper, frontend unit tests, separate-origin browser tests, separate CI stages and modular verification/startup support in the existing delivery runner. Keep backend and frontend artifacts separate.

## Acceptance criteria evidence

| Acceptance criterion | Evidence |
| --- | --- |
| AC-001 | Backend sources/tests moved to backend; UI and npm tooling in frontend; root src has no tracked files. |
| AC-002 | HomeController removed; RuntimeBoundaryIntegrationTest verifies REST-only routes; Maven packaging guard rejects UI assets in the backend JAR. |
| AC-003 | HTTP calls centralized in the API/client modules; real browser tests assert requests use the backend origin. |
| AC-004 | 31 retained mocked UI regressions and three real browser scenarios cover CRUD, search, nationality and duplicate management. |
| AC-005 | All existing backend tests retained; 146 backend tests pass. |
| AC-006 | runtimeConfig and HTTP client tests cover configured URL normalization and request construction; real E2E uses a separate backend origin. |
| AC-007 | Backend CORS tests cover approved/foreign origins, write preflights and exposed Location; browser tests cover the real boundary. |
| AC-008/009 | Module POMs and documented independent commands; both modules succeed in the reactor. Separate standalone-command execution is not claimed. |
| AC-010 | Maven Wrapper clean verify succeeds, including local toolchain installation, frozen npm installation, units and production build. |
| AC-011 | 17 frontend unit tests pass, covering HTTP/API/form/rendering/error paths. |
| AC-012 | 34 browser tests pass, including three real separate-origin scenarios with automatically owned servers. |
| AC-013 | Unit and browser markup tests verify literal rendering. |
| AC-014 | README, architecture and testing-strategy documents updated. |

## Quality evidence

- [x] Local verification: 146 backend tests, 17 frontend units and 34 browser tests passed; zero failures, errors, skips or flaky browser results.
- [x] Wrapper clean verify and E2E verification succeeded; production frontend build and backend packaging guard passed.
- [x] Independent source review recorded in docs/sdlc/reviews/AIDM-185.md.
- [ ] Final committed-head GitHub Quality gate and Browser acceptance jobs pass.
- [ ] Final staged content and publication reviewed for excluded generated files, secrets and private issue content.

Local results cover the feature working tree before its delivery commit, based on 9485d5b9e56d9c20efbafdf5f1533e0016145caa. Final-head CI must verify the published commit before merge.

## Independent review

Reviewer: independent Codex session aidm184_interactive_review. One Medium prerequisite-check finding was resolved and re-reviewed; no open actionable Medium-or-higher source findings remain. Generated wrapper and lock were separately inspected. Full report: docs/sdlc/reviews/AIDM-185.md.

## Decisions and residual risks

- The owner approved documenting public VITE_API_BASE_URL in README and supplying it through process environment instead of creating a dotenv example file. Managed file-access policies were not relaxed.
- Audit is not clean: two Moderate entries concern one Vitest/mocker advisory; one Low entry concerns esbuild's Windows built-in server. Independent review found no affected execution path in the current jsdom/static-build setup. Reassess before adding standalone mocker plugins, exposing test services or using esbuild's server. See the review report for maintainer advisory links.
- Production origins/topology remain configurable deployment choices; no actual deployment is included.
- Main has not yet been merged or launched with this change.

## Delivery metrics

| Metric | Value |
| --- | --- |
| Intake | Confirmed 2026-10-13; refreshed 2026-10-16T15:02:17Z |
| First implementation commit | Pending |
| Pull request opened | Pending |
| Merge completed | Pending human merge decision |
| Latest local verification | 2026-10-16T14:54:38Z through 14:57:12Z |
| Local result | 146 backend, 17 frontend, 34 browser tests passed |
| CI result | Pending publication |
| Human interventions | Filesystem relocation, configuration deviation approval, toolchain/bootstrap and repeated terminal verification, Jira bridge restart; exact total not instrumented |
| AI usage and cost | Unavailable |

## Human merge decision

- [ ] Required final-head CI checks are green and a human approves merging.
