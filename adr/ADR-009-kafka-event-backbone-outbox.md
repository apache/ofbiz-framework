# ADR-009: Kafka Event Backbone with Outbox (amends global RabbitMQ rule)

**Status**: Accepted
**Date**: 2026-10-16
**Authors**: Ken Semerkant + agent brainstorming session
**Related**: ADR-007, FUTURESTATE-ARCHITECTURE.md §7-8

## Context
The migration chose event-driven async processing (D7): domain events,
streams (MRP), selective event sourcing. The team's global CLAUDE.md
defaults to RabbitMQ for events; that default suits work-queue
notifications, not ordered, replayable domain event streams.

## Decision
Kafka (Strimzi on K8s) is the event backbone for this program —
**scoped amendment** to the global RabbitMQ rule. Every state change other
contexts care about is written to a per-service `outbox` table in the same
DB transaction and relayed to Kafka (atomic dual-write avoidance). Topics
`<context>.<aggregate>.<action>.v<N>`, Avro + schema registry, BACKWARD
compat gates. Strimzi 1.3.0, 3 brokers, RF=3/minISR=2 (verified roundtrip).

## Rationale
1. Per-aggregate ordering and replay are first-class (consumers rebuild
   projections — required by the CDC strangler, ADR-012).
2. Kafka Streams for MRP-style continuous computation.
3. Outbox gives exactly the atomicity XA gave the monolith (ServiceXaWrapper
   replacement, FUTURESTATE-LOWLEVEL §8).

## Consequences
**Positive:** replayable history; audit-svc consumes wildcard (ADR-011).
**Negative:** broker operational weight; schema governance required.

## Alternatives Considered
- **RabbitMQ (keep global default):** weaker ordering/replay story.
- **Both brokers:** two event planes, more ops, no benefit.

## References
platform/kafka/, FUTURESTATE-LOWLEVEL.md §4-5
