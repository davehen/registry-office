# Frontend and backend separation review

## Scope and evidence

Independent reviewer `aidm184_interactive_review` inspected the AIDM-185 feature branch's staged source relocations and authored module/client/CORS/test/runner changes on 2026-10-16. The reviewer read repository policy, delivery skill and sanitized intake; did not execute builds, package tools or browsers; did not access protected configuration files; and did not modify the implementation.

## Findings

One Medium finding: modular delivery still required global Maven, Node and pnpm before the wrapper/local-toolchain workflow. The implementer made prerequisite checks layout-aware. Independent re-review confirmed the finding resolved and identified no additional actionable source findings in the inspected modular verification/startup and API-preserving migration.

The reviewer found no source defect in centralized HTTP/error handling, configured CORS, separate-origin requests, existing UI regressions, REST-only packaging validation and owned-process cleanup. This is source-only approval, not a claim of successful runtime verification.

## Outstanding delivery conditions

Maven Wrapper and npm lock remain bootstrap artifacts to generate and inspect. Java, frontend unit/build and Playwright E2E results must come from actual execution. The user approved replacing the environment-example file with README/process-environment configuration on 2026-10-16; managed access policies remain unchanged. Refresh Jira requirements before publication, review the final diff/commit and require both final-head CI jobs to pass before merge. Exact reviewed/tested commit identifiers will be recorded when source and generated bootstrap files are committed.

## First execution repair review

After human-terminal bootstrap and verification on 2026-10-16, the reviewer separately approved the UI fixture raw-string import and same-major Vite/Vitest patch updates, with no actionable source findings. The UI assertions are unchanged. Backend evidence confirms 146 passing tests; frontend HTTP/API suites passed 10 tests, but the UI suite did not collect and browser execution did not start. Generated wrapper/lock inspection, lock regeneration for the new dependency pins, audit confirmation and fresh full verification remain necessary. No compatibility or clean-audit result is inferred from source review.

After subsequent lock-generation failures, the same independent reviewer inspected Vitest 4.1.0/Vite 7.3.5 and the npm 11.19.1 resolver repair. No actionable source mismatch was found: npm pins agree across Maven, package metadata, README and CI; plugin-level configuration supports the direct toolchain bootstrap goal. Vitest 4 is a major upgrade and actual compatibility remains unverified despite no obvious mismatch in the test APIs, jsdom configuration or JSON-report consumer. This approval is source-only; lock regeneration, audit and runtime tests are still required.

The next human-terminal run passed 146 backend tests, 15 frontend unit tests and the production build; 32/34 browser tests passed, including all three real separate-origin scenarios. Independent focused source review approved restoring the edit-person 404/410 status-specific messages and adding two parameterized UI regression tests; no actionable findings. Other error messages and refresh/no-invalid-form behavior remain intact. Browser assertions are unchanged. These edits still require fresh execution; three reported audit findings (one low, two moderate) require separate assessment.

Fresh verification evidence on 2026-10-16 from `registry-office-sdlc-20261016-6867-lyxyf1` confirms both reactor builds succeeded after the UI repair: 146 backend tests, 17 frontend unit tests and 34/34 browser tests, including all real separate-origin scenarios. No failures, errors, skips or flaky browser tests were reported. This is uncommitted working-tree evidence; final-head CI, final bootstrap/diff review, refreshed Jira and dependency-advisory assessment remain outstanding. No publication, merge or release success is claimed.

## Final bootstrap review and advisory assessment

Independent reviewer `aidm184_interactive_review` inspected the generated wrapper and final lock without executing package code. Wrapper 3.3.2 selects Maven 3.9.9 over HTTPS; the Unix wrapper is executable. Lock root dependency pins match package.json; all 135 dependency entries have integrity hashes and public npm registry resolutions. No actionable Medium-or-higher source finding was identified.

A read-only npm bulk-advisory query with only public package names/versions identified the three previously reported audit entries: Vitest and @vitest/mocker each reference the same Moderate advisory [GHSA-82fw-gwwq-j7x9](https://github.com/vitest-dev/vitest/security/advisories/GHSA-82fw-gwwq-j7x9); esbuild references Low advisory [GHSA-g7r4-m6w7-qqqr](https://github.com/evanw/esbuild/security/advisories/GHSA-g7r4-m6w7-qqqr). This metadata query is not a replacement for a complete npm audit execution.

The Vitest advisory's unauthenticated path requires the standalone mocker/interceptor plugins; none are used here. Unit tests use jsdom, not Vitest browser mode; Vite's app server does not load those plugins. The esbuild advisory concerns its built-in development server on Windows; this project does not invoke that server. The independent reviewer confirmed no affected execution path in the inspected setup and no demonstrated application Medium finding requiring a design decision. Audit is **not clean**; retain these dependency risks and reassess before enabling those modes, exposing test services, or introducing esbuild's server. No tests were weakened or audit results suppressed. Refreshed Jira, a final committed head and final-head CI remain delivery gates.
