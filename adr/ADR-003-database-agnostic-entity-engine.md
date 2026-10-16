# ADR-003: Database-Agnostic Entity Engine over ORM

**Status**: Accepted (historical)
**Date**: 2026-10-15 (reconstructed)
**Authors**: Apache OFBiz community (original decision); reconstructed by agent from working tree
**Related**: ADR-001

> **Reconstruction note** — see ADR-001.

## Context

ERP deployments arrive with an incumbent database (Oracle shop, MSSQL shop, MySQL shop…). Forcing a database choice blocks adoption. When OFBiz's data layer was designed, mainstream ORMs did not offer the portability + generic-tooling combination the framework needed, and the entity model must stay declarative (ADR-001).

## Decision

Build a custom persistence layer — the Entity Engine (`framework/entity`) — instead of adopting JPA/Hibernate:

- `Delegator` interface (Delegator.java:52) serves all CRUD/find operations; `GenericDelegator` is the implementation (factory-only construction).
- `ModelReader` (ModelReader.java:105) loads entity XML at startup into `ModelEntity` objects; SQL is generated generically.
- Portability via abstract field types: `framework/entity/fieldtype/fieldtype{postgres,mysql,oracle,mssql,sybase,h2,hsql,firebird,sapdb,advantage,axion,daffodil}.xml` map types like `id-ne`, `currency-amount` to dialect SQL types.
- Datasources/delegators configured in `framework/entity/config/entityengine.xml`.

## Rationale

1. **Portability with control** — 12 dialects supported by XML mapping alone; adding a dialect never touches Java.
2. **Generic tooling** — because all access flows through one API, webtools can browse/edit any entity generically; services can be auto-generated from entities (`EntityAutoEngine`).
3. **Era fit** — pre-JPA; EJB entity beans were the standard alternative and were heavier.

## Consequences

**Positive:** single query surface, uniform caching (entity caches in entityengine.xml), view entities join at mapping level; no ORM upgrade churn.
**Negative:** no JPA ecosystem (criteria APIs, JPA tools, dialect maintenance from vendors); team owns SQL generation for every dialect; modern ORM features (lazy loading graphs, dirty tracking conventions) absent by design.
**Trade-offs:** full control and portability vs. community-maintained ORM machinery.

## Alternatives Considered

- **EJB entity beans (contemporary option):** heavier, container-coupled; rejected.
- **JPA/Hibernate (retrospective):** would reduce custom-code burden but breaks the generic-tooling single-API premise and arrived after the architecture was set.

## References

- ARCHITECTURE.md §5.8, §8; LOWLEVEL.md §4
- `framework/entity/config/entityengine.xml`, `framework/entity/fieldtype/`
