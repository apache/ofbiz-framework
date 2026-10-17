# ADR-011: Observability From Inception (Jaeger, Prometheus, Grafana/Loki, OpenSearch)

**Status**: Accepted
**Date**: 2026-10-17
**Authors**: Ken Semerkant (program decision) + agent (Phase 0 execution)
**Related**: ADR-007, ADR-009, platform/observability/

## Context
Debugging and auditability during a multi-year strangler migration require
visibility from the first service deployed — not retrofit. Program decision:
stand up the full observability stack in Phase 0, before any domain service.

## Decision
Deploy and verify in Phase 0:
- **Jaeger** (OTLP): distributed tracing; the architecture-primary path is
  per-service OTel SDK -> OTLP (every service instruments from day one).
- **Prometheus**: metrics (istiod, exporters; later /actuator/prometheus).
- **Grafana**: dashboards; datasources provisioned for all four stores.
- **Loki + promtail**: log aggregation for dashboards (containerd paths).
- **OpenSearch + fluent-bit**: long-retention log analytics + audit queries;
  index ofbiz-logs. (fluent-bit chosen empirically: filebeat 8.x and 7.17
  both fail OpenSearch 2.x on ES-license handshake / _type removal.)

Known issue logged (KNOWN-ISSUE-1): Istio 1.31 minimal-profile mesh-span
emission not firing; service-level OTel SDK tracing is unaffected and is
the contract services must implement.

## Rationale
1. Debugging distributed failures without traces is guesswork.
2. Audit trail (audit-svc + OpenSearch retention) is a migration exit
   criterion, not an afterthought.
3. Same stack locally (kind) and in cloud — profile swap only.

## Consequences
**Positive:** every story's evidence includes traces/metrics/logs links.
**Negative:** stack maintenance; double log stores (Loki dashboards vs
OpenSearch retention) accepted deliberately.

## Alternatives Considered
- **OTel Collector as sole pipeline:** planned for cloud profile; direct
  exporters kept locally for simplicity.
- **ELK instead of OpenSearch+Loki:** heavier; OpenSearch chosen (already
  the program's search engine choice, D-series).

## References
platform/observability/, IMPLEMENTATION.md P0-F3
