# ADR-013: Clean Architecture Per Service, Java 21, Database-per-Service

**Status**: Accepted
**Date**: 2026-10-16
**Authors**: Ken Semerkant + agent brainstorming session
**Related**: ADR-007, global CLAUDE.md (Architecture section)

## Context
The team's global standard mandates Clean Architecture layering; the
migration target needed language/runtime and persistence decisions (D4,
D10) consistent with it.

## Decision
Every service is a Maven multi-module project:
`<svc>-domain` (pure Java, zero Spring), `<svc>-application` (use cases,
transactions, outbox writes), `<svc>-adapters` (REST/gRPC/Kafka),
`<svc>-infrastructure` (JPA, config, clients). Dependencies point inward
only — enforced by ArchUnit in CI. Java 21 (virtual threads for blocking
I/O), Spring Boot 3.3.x. Database-per-service: Postgres 16 each, Flyway
migrations, no shared persistence module, no cross-DB joins; cross-context
references are IDs validated by events.

## Rationale
1. Domain purity keeps business logic testable without containers and
   portable across frameworks.
2. Virtual threads remove reactive complexity for I/O-bound services.
3. DB-per-service is the hard boundary that makes services replaceable.

## Consequences
**Positive:** enforced layering (ArchUnit); independent schemas.
**Negative:** no cross-service joins — composition at gateway/SPA;
migration of the 904 entities requires the ADR-012 bridge.

## Alternatives Considered
- **Modulith with shared DB:** rejected (see ADR-007).
- **Kotlin/Go services:** rejected for team Java continuity.

## References
FUTURESTATE-LOWLEVEL.md §1, §2; CLAUDE.md (root)
