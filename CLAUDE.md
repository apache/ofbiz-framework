# CLAUDE.md — OFBiz Modernization Program (repo rules)

This repository is a migration program: `monolith/` (Apache OFBiz, current
system) being replaced by `microservices/` (target, per
[FUTURESTATE-ARCHITECTURE.md](FUTURESTATE-ARCHITECTURE.md)). These rules
govern ALL new development. The monolith's own upstream conventions apply
only inside `monolith/`.

## Document authority (read before working)

1. [IMPLEMENTATION.md](IMPLEMENTATION.md) — stories/gates (execution authority)
2. [FUTURESTATE-ARCHITECTURE.md](FUTURESTATE-ARCHITECTURE.md) + [FUTURESTATE-LOWLEVEL.md](FUTURESTATE-LOWLEVEL.md) — target design
3. [adr/](adr/) — decisions ADR-001..014; do not re-litigate, supersede via new ADR
4. [ARCHITECTURE.md](ARCHITECTURE.md)/[LOWLEVEL.md](LOWLEVEL.md)/[PRD.md](PRD.md) — current monolith (reference only)

## Architecture rules (ADR-007, ADR-013)

- **Clean architecture per service, enforced:** `domain` (pure Java, zero
  Spring) ← `application` ← `adapters`/`infrastructure`. ArchUnit test in
  every module; inward-only dependencies.
- **16 services** (11 domain + 5 platform). New capability goes in the
  owning bounded context — never invent a 17th service without an ADR.
- **Java 21, Spring Boot 3.3.x**, virtual threads enabled.
- **Database-per-service.** No cross-DB joins, no shared persistence
  module. Cross-context data = API call or event; references are IDs.
- **Persistence:** JPA/Hibernate + Flyway. Entity XML/Delegator patterns
  are monolith-only.

## Event rules (ADR-009)

- Kafka is the event backbone for this program (scoped amendment to the
  global RabbitMQ default; RabbitMQ remains valid in other projects).
- State changes other contexts care about ⇒ write to the service `outbox`
  table **in the same transaction**; relay publishes. No direct dual-writes.
- Topics: `<context>.<aggregate>.<action>.v<N>`; Avro + schema registry;
  BACKWARD compatibility gate in CI. Consumers must be idempotent
  (`processed_events` table).

## Storage rules (ADR-008)

- Services code against the **S3 API only**. Local: Garage
  (`platform/storage/`). Cloud: S3 endpoint+creds swap. No local-disk or
  DB BLOB storage.

## Observability rules (ADR-011)

- Every service ships OTel SDK from day one: traces → Jaeger (OTLP),
  metrics → `/actuator/prometheus`, structured JSON logs.
- Story evidence links a Jaeger trace, a metrics query, and a log query.
- Audit events go to Kafka (audit-svc consumes wildcard; OpenSearch
  retention).

## Deployment rules (ADR-010)

- Kubernetes + Istio (STRICT mTLS). Local = kind cluster
  (`platform/bootstrap/`). East-west = gRPC (mesh); north-south = REST via
  gateway only.
- Identity: OIDC JWT from identity-svc; no service issues its own tokens.

## Process rules

- Work only IMPLEMENTATION.md stories (or add stories there first via PR).
- Definition of Done: AC met with evidence link, CI green, contracts
  backward-compatible, dashboards updated, docs updated same-PR.
- Every cutover: staging → consistency delta 0 → rollback drill → flip.
- Verify infra with the `platform/*/20-verify.sh` scripts; they are the
  gate evidence generators.
