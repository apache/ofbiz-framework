# AGENTS.md — Agent Working Rules (OFBiz Modernization Program)

Companion to [CLAUDE.md](CLAUDE.md) (architectural rules — also binding).
This file adds agent-specific execution discipline. You wrote "AGENT.md";
`AGENTS.md` is the cross-tool convention adopted here.

## Non-negotiables

1. **Read before acting:** CLAUDE.md → IMPLEMENTATION.md (current phase +
   your story) → FUTURESTATE-LOWLEVEL.md sections the story touches.
2. **Story-scoped work only.** No story in IMPLEMENTATION.md = no code.
   Propose the story (issue file + PR) instead of building speculatively.
3. **AC = done.** A story is complete when every checkbox in its
   `issues/<id>.md` has evidence (command output, trace link, screenshot).
   "Should work" is not evidence.
4. **Gates beat calendars.** Phase gates (`issues/P<n>-GATE.md`) close
   only with all boxes checked; never self-check without running the
   matching verifier script.

## Where things live

| Need | Location |
|---|---|
| Story list + AC | `issues/` (index in `issues/README.md`) |
| Target service specs | FUTURESTATE-ARCHITECTURE.md §5-6 |
| Module scaffolding, DDL, contracts | FUTURESTATE-LOWLEVEL.md |
| Infrastructure + verifiers | `platform/<area>/` — run `20-verify.sh` for evidence |
| Current-monolith questions | ARCHITECTURE.md / LOWLEVEL.md / PRD.md |
| Decisions | `adr/ADR-001..014` — supersede, never silently contradict |

## Hard technical constraints (violations fail review)

- New service code: clean-arch modules, domain layer Spring-free (ArchUnit
  enforced). Java 21 / Boot 3.3.x.
- Cross-service data: gRPC call or Kafka event — never another service's DB.
- Events via outbox table in-transaction; topic naming + Avro registry;
  idempotent consumers.
- Storage: S3 API only (local Garage).
- Every service: OTel traces/metrics/logs from first commit.
- No changes inside `monolith/` except via strangler bridge stories
  (reverse-sync adapters, gateway host config).

## Verification discipline

- Run the relevant `platform/*/20-verify.sh` after infra-affecting work;
  paste tail as evidence.
- Unit: domain modules >90%. Slice tests with Testcontainers (Postgres +
  Kafka). Contract tests published when APIs/events change.
- Style gates run on push (root `gradlew` shim → monolith checks apply to
  monolith paths only; new service repos carry their own lint).

## Commit discipline (INVEST)

- One story (or story-slice) per commit; message references the story ID
  (e.g. `P0-F3 observability stack`) and states verification evidence in
  the body.
- Push after each green unit; never stack unverified work.
