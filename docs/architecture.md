# Frontend and backend architecture

The repository builds two independent applications. The Maven aggregator orders backend and frontend builds but does not introduce a runtime dependency between them.

The Spring Boot backend owns persistence, business validation, version checks, duplicate matching and atomic merge/history. Controller, Service and Repository packages and all REST paths remain unchanged. It neither serves nor packages the application's frontend. OpenAPI and its Swagger UI remain backend documentation endpoints, not the registry application UI.

The browser frontend owns display, forms and interaction. Its ES-module dependency chain is main → UI modules → civilRegistryApi → httpClient → runtimeConfig. API data enters DOM text nodes; no backend field is evaluated as markup. The HTTP client owns configured URL construction, JSON parsing and typed status/network errors. No frontend module imports Java or accesses H2.

VITE_API_BASE_URL is a public build-time API base, with trailing slashes normalized and credentials/query/fragment rejected. Supply it through the build process environment. An empty value supports a deliberately configured same-origin proxy; separate-origin development and deployment must set the backend URL. No actual deployment origin is hardcoded in frontend source.

Central backend CORS configuration accepts exact configured origins for person, duplicate and OpenAPI paths. It grants required REST methods and Content-Type, exposes Location and never enables credentials. Unapproved origins and headers do not receive access. Do not treat CORS as authentication; this demo has no authentication.

Maven verify builds a backend executable JAR and frontend/dist independently. The backend packaging guard rejects frontend UI resources in BOOT-INF/classes; Swagger dependency resources are unaffected. The frontend module installs a pinned local Node/npm toolchain, uses the committed npm lock, and never copies assets to backend. Existing generated reference output is not used.

See the [runtime boundary decision](sdlc/decisions/AIDM-185-runtime-boundary.md) and [testing strategy](testing-strategy.md).
