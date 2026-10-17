# ADR-007: Target Architecture — Microservices by Bounded Context

**Status**: Accepted
**Date**: 2026-10-16
**Authors**: Ken Semerkant + agent brainstorming session
**Related**: ADR-001, ADR-003, FUTURESTATE-ARCHITECTURE.md §2 (D1-D4, D10)

## Context
The OFBiz monolith (28 components, shared DB, single JVM) cannot scale or
evolve independently per business domain. Migration decisions were locked in
the 2026-10-16 brainstorming session across ten dimensions (FUTURESTATE §2).

## Decision
Migrate to 16 independently deployable services on DDD bounded contexts:
11 domain services (party, product, order, billing, finance, inventory,
manufacturing, content, workeffort, humanres, marketing) + 5 platform
services (identity, gateway, search, notification, audit). Stack: Java 21,
Spring Boot 3.3.x, clean architecture per service
(domain/application/adapters/infrastructure), database-per-service
(JPA/Flyway; 904 entities split by ownership per FUTURESTATE-LOWLEVEL §2).

## Rationale
1. Domain-aligned ownership enables independent deployability (D1/D3).
2. DB-per-service removes the shared-schema coupling that prevents change.
3. Clean architecture keeps domain logic framework-free per program rules.

## Consequences
**Positive:** independent scaling/deployment; technology isolation; team ownership.
**Negative:** distributed transactions (solved by outbox+sagas, ADR-009);
operational surface (solved by platform services + mesh, ADR-010/011).
**Trade-offs:** eventual consistency between contexts replaces in-process ACID.

## Alternatives Considered
- **Modular monolith refactor:** lower cost, keeps single deployable — rejected:
  does not meet independent-deployment goal.
- **Fine-grained services (20-30):** max isolation, rejected: operational cost.

## References
FUTURESTATE-ARCHITECTURE.md, IMPLEMENTATION.md, PROJECT-PLAN.md
