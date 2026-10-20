# ADR-015: Spring Boot 4.0 (security-forced bump from 3.3)

- Status: Accepted
- Date: 2026-10-20
- Supersedes: the "Spring Boot 3.3.x" pin in ADR-013 / FUTURESTATE-LOWLEVEL §1 (all other ADR-013 provisions stand)

## Context

The CI image-scan gate (trivy, fail on HIGH/CRITICAL; P0-F4-S1) began
failing on the demo-svc image in Oct 2026. Root causes, in layers:

1. The distroless `java21-debian12` base carried OS-level CVEs (libexpat
   et al.) — fixed by moving to `java21-debian13` (clean).
2. Spring Boot 3.3.x (OSS support ended 2025) pins Spring Framework 6.1,
   Tomcat 10.1, Jackson 2.17 — all with open HIGH/CRITICAL CVEs whose fixed
   versions exist only on later lines (Framework 6.2/7.0, Tomcat 11).
   No combination of overrides keeps Boot 3.3 clean.

Holding the 3.3 pin means a permanently red CI gate; the pin and the gate
cannot both hold. The gate is the program's security contract
(IMPLEMENTATION.md DoD), the pin was a stack choice.

## Decision

1. Move services to **Spring Boot 4.0.x** (Java 21 unchanged; virtual
   threads unchanged). Version lives in `gradle/libs.versions.toml`
   (`spring-boot = 4.0.6` as of this ADR).
2. Keep a minimal set of **patch-ahead overrides** in the
   `ofbiz-msa.spring-boot-service` convention plugin (dependency-management
   DSL) whenever the managed BOM is one patch behind trivy's fixed
   versions. The block documents itself and shrinks as the BOM catches up.
3. Base image: distroless `java21-debian13` (or successor with 0 HIGH/
   CRITICAL findings).

## Consequences

- Boot 4 / Framework 7 renames some starters and Jackson coordinates
  (`tools.jackson.*` for Jackson 3). Service scaffolding absorbs this once,
  in the convention plugin — individual services don't feel it.
- Every new CVE wave is a BOM-patch bump (or override row), not an
  architecture event.
- FUTURESTATE-LOWLEVEL §1 stack table: read "Spring Boot 3.3.x" as
  "Spring Boot 4.0.x (ADR-015)".
