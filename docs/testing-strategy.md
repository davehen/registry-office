# Full stack testing strategy

## Normal verification

`./mvnw clean verify` builds both modules. `./mvnw -pl backend clean verify` runs backend tests and packaging without Node. `./mvnw -pl frontend clean verify` installs pinned Node/npm, runs npm ci, frontend unit tests and a standalone production build without Java backend execution.

All existing backend tests were relocated rather than replaced. Additional tests cover REST-only routing, OpenAPI, configured CORS origins, methods, headers and exposed Location. Packaging verification inspects the actual executable JAR for application frontend assets.

Frontend Vitest/jsdom tests mock fetch, not Spring. They cover configured URL normalization, encoded API paths, methods/bodies, parsing and error responses, empty/invalid payloads, network failures, optional nationality/form mapping, table rendering, literal markup, loading/error/empty state, stale list requests and duplicate-submit prevention. JSON results are written to frontend/target/vitest-results.json.

## Browser verification

`./mvnw clean verify -Pe2e` adds Playwright. After backend packaging, the frontend Maven profile installs Chromium and runs npm run test:e2e. On Linux, browser OS libraries must be available; CI installs them explicitly before the profile.

Playwright owns three test servers and stops them afterward: clean in-memory backend on 127.0.0.1:18081; mocked UI on 127.0.0.1:4173; real frontend on 127.0.0.1:4174. Backend CORS admits only the real frontend origin. Servers are never reused, so a conflicting occupied test port fails rather than operating on another app's database.

Mocked browser tests retain the prior nationality and duplicate-management regression assertions, adapting only the ES-module entry points. Real browser journeys cover complete CRUD, partial/nationality/no-result search, duplicate/error display, deletion by ID/email/all, duplicate merge, retired-record lookup and immutable audit history. Network assertions verify browser REST traffic targets the backend origin. Real tests run serially against their owned database and use synthetic records; fixture cleanup does not touch an existing application.

Reports, failure screenshots and retained failure traces are under frontend/target. Playwright JSON results must include passed tests from both mock and real projects. CI runs normal verification and browser acceptance in separate jobs and uploads diagnostics even on failure. Both jobs must pass on the publication head.

## Delivery evidence

The monolithic SDLC script detects the modular layout. Its `--verify-checkout` mode needs no Jira credentials or nested Codex session and records actual Java, frontend-unit and browser totals. It does not approve its own source, publish a PR or merge. Dirty source verification is labeled as such; the independent reviewer and final-head CI remain required.

At the initial implementation checkpoint, runtime builds, wrapper generation and npm lock creation remain unverified because the agent's platform rejected execution of downloaded package code. A human terminal verification is required; source inspection and syntax checks are not equivalent to passing tests.
