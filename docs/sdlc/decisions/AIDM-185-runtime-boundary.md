# Separate frontend and backend runtimes

## Decision

For AIDM-185, use a Maven aggregator with independent backend and frontend modules. Spring Boot serves REST/OpenAPI only. Vite builds vanilla ES modules into standalone static assets; no frontend output enters the backend JAR. Existing business APIs, including duplicate management, remain unchanged.

The browser API origin is explicit build-time configuration. Backend CORS admits configured exact origins and required methods, exposes Location and never enables credentials. Person and duplicate APIs share the same policy. Same-origin proxy deployment remains possible through an explicitly empty frontend API base.

## Consequences

Deploy two artifacts and configure origins per environment. Normal Maven verify includes backend and frontend unit/build checks; browser E2E is opt-in through the e2e profile and mandatory in CI. Independent backend builds do not require Node. Frontend tests do not require Java except full-browser E2E. In-memory data durability is unchanged.
