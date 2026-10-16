# AIDM-185 frontend and backend separation

## Identity and outcome

Jira: AIDM-185, child of pilot Epic AIDM-181, confirmed from the approved read-only bridge on 2026-10-13 and refreshed at 2026-10-16T15:02:17Z. Implementation resumed 2026-10-16 on `codex/aidm-185-decouple-frontend-backend`. The refreshed requirements match the approved implementation scope, including preservation of existing features and the separately approved process-environment configuration deviation.

Deliver independently buildable/deployable REST backend and static frontend, retaining current functionality and REST contracts. The single delivery entry point does not imply a single runtime artifact.

## Requirements and acceptance criteria

| Requirement | Acceptance | Implementation and evidence plan |
| --- | --- | --- |
| BR-001 | AC-001 | Move Java/resources/tests to backend; browser assets to frontend; no obsolete root src. |
| BR-002 | AC-002 | Remove HomeController; disable backend static mappings; verify root 404 and packaged JAR boundary. |
| BR-003 | AC-003 | ES-module API client is the only HTTP boundary. |
| BR-004 | AC-004 | Preserve CRUD, search, nationality, duplicate detection/dismissal/merge/history and current design. |
| BR-005 | AC-005 | Move all existing Java tests unchanged; preserve DTOs, lifecycle and every REST operation. |
| BR-006 | AC-006 | Validate and normalize build-time VITE_API_BASE_URL, supporting configured origin/path and explicit same-origin deployment. |
| BR-007 | AC-007 | Property-driven exact-origin CORS, no credentials/wildcards; Location exposed; all person and duplicate operations supported. |
| BR-008 | AC-008/009 | Backend-only Maven and frontend-only npm commands; separate artifacts. |
| BR-009 | AC-010 | Maven Wrapper, aggregator and pinned local Node/npm installation; frozen npm lock. |
| BR-010 | AC-011/012 | Vitest/jsdom unit/integration tests; Playwright mocked regressions plus real separate-origin CRUD and duplicate journeys; E2E profile only. |
| BR-011 | AC-013 | DOM text nodes for server data; malformed/empty/error/network response coverage. |
| BR-012 | AC-014 | README, architecture and testing commands; update CI and delivery runner paths. |

## Scope and decisions

No business-rule, database, authentication, UI-framework or visual redesign. Existing in-memory database limitations remain. Generated reference trees are not implementation inputs. Prior descriptions of the old UI do not justify dropping newer features.

The supplied architecture and technical choices are approved for this refactoring. Production origins/reverse-proxy topology are non-blocking deployment decisions. Environment values are public build-time configuration, never secrets. Managed file-access restrictions still apply, including environment example files; use documented environment variables rather than silently accessing protected files.

## Technical and test plan

1. Root Maven aggregator retains group/version and Java 17/Spring Boot 3.3.4 management; modules ordered backend then frontend. Backend has no frontend dependency.
2. Backend retains current dependencies, packages, tests and API semantics. Add central typed CORS configuration and boundary/API/CORS tests. Packaging verification rejects application UI assets while preserving Swagger dependency resources.
3. Frontend uses Vite, Vitest/jsdom and Playwright. Extract runtime configuration, HTTP parsing, person/duplicate API client and DOM-only UI modules. Replace inline handlers with event listeners. Prevent repeated mutation submissions and preserve error feedback.
4. Normal root verify installs pinned Node/npm, runs npm ci, frontend tests and production build. E2E profile runs browser tests with automatically owned separate-origin servers and clean test data; CI runs normal and E2E verification separately.
5. Keep previous browser regressions and add complete real-browser create/edit/search/delete/error flows, nationality and duplicate merge. Assert requests target the backend origin and CORS preflight succeeds.
6. Independent review follows implementation; runtime verification is not inferred from source review. No merge before actual required CI checks pass on the final head.

## Evidence

Filesystem move performed by the human after this session's directory rename was denied. No build, test, review or publication success is yet claimed. Dedicated ADR: `docs/sdlc/decisions/AIDM-185-runtime-boundary.md`.

Implementation checkpoint 2026-10-16: module POMs, REST-only routing/CORS, API/DOM ES modules, retained browser regressions, new frontend unit tests and real cross-origin browser journeys, CI jobs and modular monolithic-runner verification/startup are authored. Static XML/JSON parsing, selected JavaScript/Ruby syntax checks and 24 native mocked runner tests (129 assertions) pass. These are not product runtime results.

Independent reviewer `aidm184_interactive_review` performed a separate source inspection of AIDM-185. One Medium finding (obsolete global Maven/Node/pnpm preflight requirements) was corrected and re-reviewed; no open source findings remain in the inspected implementation. Wrapper and npm lock generation, full Java/frontend/browser execution, refreshed Jira requirements and final-head GitHub CI remain pending.

The platform rejected executing Maven Wrapper's downloaded registry plugin as untrusted package code. No bypass or retry was attempted. The user ran bootstrap in their terminal and generated the wrapper and npm lock. Verification run `registry-office-sdlc-20261016-70255-i6t7qk` passed all 146 backend tests with zero failures/errors/skips and the backend packaging guard. Two frontend suites passed 10 tests; the UI suite failed during collection because its file reader received a transformed non-file URL. The repair imports the existing HTML fixture with Vite's raw-string loader without weakening assertions. Initial npm installation reported four vulnerabilities; Vite/Vitest were updated within their current majors to 7.3.5/3.2.5, respectively, following official patch advisories. Lock regeneration, current audit and fresh frontend/reactor/browser verification remain required.

Creating `frontend/.env.example` is separately blocked by managed patterns H02/W02. On 2026-10-16 the user explicitly approved the narrow specification deviation: document VITE_API_BASE_URL in README and supply it through the process environment, without dotenv files. No security policy was relaxed.

The attempted Vitest 3.2.5 lock update failed before verification: npm reported ETARGET for its exact vite-node@3.2.5 dependency. Vitest was therefore advanced to 4.1.0, an officially patched release. Read-only HTTPS metadata checks against the actual npm registry confirmed Vitest 4.1.0 and Vite 7.3.5 availability, compatibility with the pinned Node version, and absence of the missing vite-node dependency in Vitest 4.1.0. Actual dependency resolution, audit and test execution remain pending; no package code was executed by these metadata checks.

The subsequent lock-only update crashed inside npm 10.9.2's Arborist `loadPeerSet` (`edgesOut` on a null parent), before any new verification run. The log's optional-peer fetch chain matches the reproduction reported in https://github.com/npm/cli/issues/9787. The repository-local npm pin and CI bootstrap were advanced to 11.19.1, for which successful reproductions are reported there; public registry metadata confirms compatibility with Node 22.16.0. This is a proposed resolver repair, not a successful local installation or test result. No cache, lock or dependency directory was deleted; no force/legacy-peer-deps workaround was introduced. Human-terminal toolchain installation, lock regeneration and full verification remain pending.

Human-terminal npm 11.19.1 bootstrap and lock regeneration succeeded. Verification run `registry-office-sdlc-20261016-92426-bqwmv1` passed the normal reactor build, all 146 backend tests, all 15 frontend unit tests and the Vite production build. Browser execution passed 32 of 34 tests, including all three real separate-origin scenarios; two retained mocked regressions failed because edit-person errors displayed a generic backend message instead of the existing 404/410 unavailable/merged explanation. The edit UI now restores those status-specific explanations while retaining list refresh and not opening the form on failure. Two focused unit regressions were added without weakening the browser assertions. Fresh verification is required after this repair. Lock-generation audit reported three remaining vulnerabilities (one low, two moderate); detailed assessment remains pending.

Fresh human-terminal verification `registry-office-sdlc-20261016-6867-lyxyf1` completed successfully on 2026-10-16 (started 14:54:38Z; E2E build finished 14:57:12Z). Normal clean reactor verification and E2E verification both succeeded: 146 backend tests, 17 frontend unit tests and all 34 browser tests (31 mocked, three real separate-origin), with zero failures, errors, skips or flaky browser results. Production frontend build and backend packaging checks passed. This evidence covers the dirty feature working tree based on `9485d5b9e56d9c20efbafdf5f1533e0016145caa`, not a committed final head. Remaining delivery gates: dependency-advisory assessment, refreshed Jira intake, generated bootstrap/final diff review, commit/publication and final-head CI before any merge or launch from main.

Final independent bootstrap review found no actionable Medium-or-higher source finding. The remaining audit entries were assessed against maintainer advisories: standalone Vitest mocker plugins and esbuild's Windows built-in server are not used by this setup. Audit remains non-clean (two Moderate entries for the same Vitest advisory and one Low esbuild entry); assessment and conditions for reassessment are recorded in the independent review. The previous Jira snapshot is absent: refresh the approved Jira bridge before publication. No PR or merge has been performed.

Approved bridge refreshed at 2026-10-16T15:02:17Z; AIDM-185 remains under AIDM-181 with no new blocking Must ambiguity. Description text SHA256: `2c4c0d781b2591c3ac0c1abfb7148ead590fe622249d89c89da24f70021a2e84`. Raw description remains outside Git. The current session's GitHub CLI returned HTTP 401 during a read-only identity check, so publication and remote CI remain blocked here. No credentials were read or copied to work around this limitation.
