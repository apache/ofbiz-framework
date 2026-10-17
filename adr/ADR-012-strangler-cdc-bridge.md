# ADR-012: Strangler Migration via Debezium CDC + Reverse-Sync

**Status**: Accepted
**Date**: 2026-10-16
**Authors**: Ken Semerkant + agent brainstorming session
**Related**: ADR-007, ADR-009, FUTURESTATE-LOWLEVEL.md §7

## Context
A big-bang rewrite of a 904-entity ERP is unshippable; the monolith must
keep serving unmigrated domains throughout (D2). Data must remain
consistent across the boundary during the multi-year transition.

## Decision
Strangler pattern behind the gateway: default route -> monolith; migrated
contexts carved out per phase. Data interop: Debezium CDC on the monolith
DB -> `ofbiz.<entity>.<action>` Kafka topics -> migrating services'
projection tables. After write-cutover, a reverse-sync adapter (ACL) writes
new-service events back to monolith tables for not-yet-migrated consumers.
Consistency checker (row counts + checksums) gates every cutover; rollback
= gateway route flip with CDC reverse integrity check.

## Rationale
1. Incremental value delivery; no feature freeze (PROJECT-PLAN A4).
2. CDC gives ordered, replayable migration feeds on the ADR-009 backbone.
3. Rollback drill precondition makes every cutover reversible.

## Consequences
**Positive:** reversible phases; parallel old/new operation.
**Negative:** bridge code is temporary tech debt deleted in Phase 7;
double-write windows need the consistency job continuously.

## Alternatives Considered
- **Greenfield rewrite:** simpler doc, unshippable risk.
- **Dual-write at call sites:** races; CDC is source-true.

## References
IMPLEMENTATION.md Phase 2 (template phase), PROJECT-PLAN.md §5 R2/R9
