# ADR-002: Embedded Tomcat via CatalinaContainer

**Status**: Accepted (historical)
**Date**: 2026-10-15 (reconstructed)
**Authors**: Apache OFBiz community (original decision); reconstructed by agent from working tree
**Related**: ADR-005

> **Reconstruction note** — see ADR-001.

## Context

OFBiz must run as a self-contained application: users download, configure a database, and start — without installing and administering a separate servlet container. Deployment simplicity for downstream business deployments outweighed container-standardization concerns.

## Decision

The system boots its own web server in-process. `Start` (framework/start) launches configured "containers"; `CatalinaContainer` (framework/catalina/.../container/CatalinaContainer.java) runs an embedded Apache Tomcat and mounts every webapp declared via `webapp` elements in each component's `ofbiz-component.xml`.

## Rationale

1. **Single-command startup** — `./gradlew ofbiz` yields a running system; no external app-server install, deployment descriptors, or admin consoles.
2. **Component-driven mounting** — webapps register declaratively like entities/services; one mechanism for everything a component contributes.
3. **Version control** — Tomcat version pinned by the project's build, not by whatever the operator installed.

## Consequences

**Positive:** minimal operational footprint; deterministic runtime; simple Docker story (`docker/`).
**Negative:** no external servlet container features (JNDI resources as ops would expect, container-managed clustering); ops model differs from common enterprise Java deployments.
**Trade-offs:** deployment simplicity vs. infrastructure integration.

## Alternatives Considered

- **WAR into external Tomcat/JBoss/WebSphere:** supported historically but secondary; loses single-command startup, adds container-version drift.
- **Reactive/embedded alternatives (Jetty, Netty):** Jetty was used for tests historically; Tomcat retained as the production container.

## References

- ARCHITECTURE.md §5.2, §11; LOWLEVEL.md §1
- `framework/catalina/src/main/java/org/apache/ofbiz/catalina/container/CatalinaContainer.java`
