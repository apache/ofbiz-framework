# ADR-005: JAX-RS REST API Mapped onto the Service Engine

**Status**: Accepted (historical, actively developed)
**Date**: 2026-10-15 (reconstructed)
**Authors**: Apache OFBiz community; reconstructed by agent from working tree
**Related**: ADR-001, ADR-004

> **Reconstruction note** — see ADR-001.

## Context

Consumers need headless access to OFBiz (custom frontends, integrations) without scraping HTML screens. SOAP existed (`SOAPClientEngine`) but modern consumers expect JSON over HTTP with OpenAPI documentation. Business logic already lives in services (ADR-001) — duplicating it in HTTP handlers would fork the codebase.

## Decision

Add `monolith/framework/rest-api`: a JAX-RS application (package `org.apache.ofbiz.ws.rs`) that maps HTTP requests onto existing services rather than implementing logic:

- `ApiContextListener` bootstraps dispatcher/delegator into the servlet context.
- `ServiceRequestWorker` / `RestServiceUtil` translate HTTP request → service invocation.
- `APIAuthFilter` (token), `HttpBasicAuthFilter`, `APICorsFilter` handle auth/CORS; `JWTManager` (monolith/framework/webapp) issues tokens.
- Responses use uniform envelopes (`Success`/`Error`/`ResponseStatus`, `RestListResponseBuilder` for pagination).
- OpenAPI documentation generated via `OpenApiUtil`; Swagger UI served from component-local npm tooling (`package.json`, `node_modules`).

## Rationale

1. **Single source of truth** — every endpoint executes the same service the UI uses; no logic fork.
2. **Standards** — JAX-RS + OpenAPI rather than a bespoke HTTP layer.
3. **Declarative surface growth** — endpoints follow service definitions, keeping the extension model uniform.

## Consequences

**Positive:** headless ERP consumption; OpenAPI client generation; service-level auth reuses web security plumbing.
**Negative:** REST semantics strained through generic service map in/out (pagination/filtering need dedicated builders); component carries a Node/npm dependency in a Java framework; XML-first services don't always map cleanly to resource shapes.
**Trade-offs:** API uniformity vs. REST idiomatic purity.

## Alternatives Considered

- **Expose services via generic SOAP only:** legacy; poor modern tooling story.
- **Hand-written MVC controllers:** duplicates service logic, loses uniformity.
- **GraphQL:** considered in community discussions; JAX-RS retained for standards fit.

## References

- ARCHITECTURE.md §5.5, §6.2, §7; LOWLEVEL.md §7
- `monolith/framework/rest-api/src/main/java/org/apache/ofbiz/ws/rs/`
