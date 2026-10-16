# ADR-006: FtlWriter Calls Every Macro Through One Constant Template

**Status**: Accepted (trunk)
**Date**: 2026-10-15
**Authors**: Apache OFBiz committers (OFBIZ-13675); recorded by agent
**Related**: ADR-001

## Context

Widget rendering calls theme FreeMarker macros (renderScreenField, renderTextField, …). Historically each macro invocation could go through its own template-processing path. That means repeated template parsing/lookup at render time and per-macro performance variance on every screen render — a hot path, since every back-office page renders dozens of macros.

## Decision

`FtlWriter` (framework/widget renderer infrastructure) invokes every macro through one constant, pre-parsed template (trunk commit `c0513fb2f8`, "Improved: FtlWriter calls every macro through one constant template (OFBIZ-13675)"). Macro name and parameters are passed dynamically; the template wrapper is shared.

## Rationale

1. **Hot-path economics** — one template parse/lookup instead of per-macro; benefits every screen render across all themes.
2. **Uniform behavior** — a single code path for macro invocation simplifies debugging and future optimizations (e.g. caching decisions).
3. **No theme changes** — themes keep defining the same macros; only the invocation plumbing changed.

## Consequences

**Positive:** faster widget rendering; less code duplication in renderer infrastructure.
**Negative:** macro invocation is more indirect (constant template indirection) — contributors must know the mechanism to debug rendering.
**Trade-offs:** runtime performance and uniformity vs. slight conceptual indirection.

## Alternatives Considered

- **Per-macro direct processing (status quo ante):** simpler mentally, slower and less uniform.
- **Full renderer rewrite:** disproportionate to the goal.

## References

- ARCHITECTURE.md §5.4; LOWLEVEL.md §6
- Trunk commit `c0513fb2f8`; JIRA OFBIZ-13675
