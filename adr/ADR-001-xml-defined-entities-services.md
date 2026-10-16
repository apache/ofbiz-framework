# ADR-001: XML-Defined Entities and Services Instead of Code-First

**Status**: Accepted (historical)
**Date**: 2026-10-15 (reconstructed)
**Authors**: Apache OFBiz community (original decision); reconstructed by agent from working tree
**Related**: ADR-003, ADR-005

> **Reconstruction note**: OFBiz predates ADR practice. This record describes the framework's foundational design as evidenced by the codebase, not a documented decision process.

## Context

An ERP framework must serve many businesses with different schemas and workflows. Hard-coding entities (tables) and business operations in Java couples every customization to compilation and forces ORM/vendor choices on downstream users. The framework needed a way to declare data and behavior that non-Java specialists could read, diff, and generate tooling around.

## Decision

Entities and services are declared in XML, loaded at startup by framework engines:

- Entity definitions: `entitydef/*-entitymodel.xml` per component, registered via `entity-resource` in `ofbiz-component.xml`, loaded by `ModelReader` (framework/entity/.../model/ModelReader.java:105). 904 entities ship in `applications/datamodel/entitydef/`.
- Service definitions: `servicedef/*.xml` per component, registered via `service-resource`, dispatched through `LocalDispatcher` to one of 9 engines (Java, entity-auto, Groovy, Minilang, script, route, interface, SOAP, Clojure).

## Rationale

1. **Late binding** — schema and service surface assemble at startup from all loaded components; downstream projects extend without forking framework code.
2. **Toolability** — uniform XML enables generic admin UIs (webtools entity/service browsers), validation at load time, and code generation.
3. **Vendor neutrality** — declarative data model maps to 12 database dialects via fieldtype XML (see ADR-003).

## Consequences

**Positive:** uniform extension mechanism; 904-entity schema and thousands of services readable without an IDE; engines stay generic.
**Negative:** XML indirection lengthens the trace from URL to code (mitigated by LOWLEVEL.md lookup tables); Minilang is an OFBiz-only language to learn; definitions lack compile-time checking.
**Trade-offs:** declarative consistency traded against IDE ergonomics.

## Alternatives Considered

- **Code-first + ORM (JPA/Hibernate):** rejected as primary path — ties components to specific ORM/runtime, weaker generic-tooling story.
- **Annotation-based configuration:** anachronistic to the original design era; partially adopted later for Java-side config (e.g. `@ThreadSafe` decorators in service config model).

## References

- ARCHITECTURE.md §5.6, §5.8; LOWLEVEL.md §3, §4
- `framework/base/.../component/ComponentConfig.java:425` (resource registration)
