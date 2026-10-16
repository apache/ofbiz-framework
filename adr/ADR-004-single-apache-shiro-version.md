# ADR-004: Single Apache Shiro Version, Drop shiro-core

**Status**: Accepted (trunk)
**Date**: 2026-10-15
**Authors**: Apache OFBiz committers (OFBIZ-13685); recorded by agent
**Related**: ADR-001

## Context

The build carried multiple Apache Shiro artifacts at different versions — split dependencies (e.g. a standalone `shiro-core` alongside another Shiro version pulled in for web/crypto needs). Version splits cause classpath ambiguity, duplicated classes, and CVE-chasing across artifacts during dependency updates (Dependabot flags them independently).

## Decision

Consolidate on a single Apache Shiro version and remove `shiro-core` from the dependency set (trunk commit `5b1856083d`, "Improved: Use a single Apache Shiro version and drop shiro-core (OFBIZ-13685)"). Shiro provides hashing/crypto used by `framework/security` (login credential handling).

## Rationale

1. **One version, one CVE surface** — dependency scans and upgrades touch one line item.
2. **Classpath determinism** — eliminates duplicate-class risk from split artifacts.
3. **Minimal blast radius** — Shiro is used for crypto primitives, not session/realm management, so consolidation required no code-level security architecture change.

## Consequences

**Positive:** simpler dependency tree; predictable upgrades.
**Negative:** none observed at trunk; any downstream override of Shiro version must now consider the full artifact set.
**Trade-offs:** none meaningful — pure hygiene.

## Alternatives Considered

- **Keep split artifacts pinned to same version:** fragile; requires manual synchronization each upgrade.
- **Replace Shiro entirely:** larger change with no motivating benefit at this time.

## References

- ARCHITECTURE.md §8, §9
- Trunk commit `5b1856083d`; JIRA OFBIZ-13685
