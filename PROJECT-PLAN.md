# PROJECT-PLAN.md — OFBiz → Microservices Migration

> **PM layer over [IMPLEMENTATION.md](IMPLEMENTATION.md)** (execution authority: phases/features/stories/gates) and [FUTURESTATE-ARCHITECTURE.md](FUTURESTATE-ARCHITECTURE.md) (target design). This plan adds what IMPLEMENTATION.md deliberately excludes: timeline, staffing, milestones, risks, governance.
> **Scope of authority:** if story content or gates conflict with IMPLEMENTATION.md, IMPLEMENTATION.md wins. If schedule/staffing conflict here, this file wins until re-baselined.

---

## 1. Assumptions & Constraints

| # | Assumption | Basis |
|---|---|---|
| A1 | Team: 4-6 engineers, plus part-time accounting SME and 0.5 DevOps | confirmed 2026-10-16 |
| A2 | Timeline expressed in relative months (M1 = kickoff month) | confirmed |
| A3 | Operating model: you-build-it-you-run-it — squads own services in production from first deploy | confirmed |
| A4 | Monolith stays authoritative for unmigrated domains; no feature freeze required (strangler) | FUTURESTATE D2 |
| A5 | Staging K8s cluster available from M1 (cloud budget approved) | Phase-0 entry criteria |
| A6 | ~20% capacity loss to interruptions/support of monolith in parallel | conservative |
| C1 | Hard dependency: accounting entity split needs SME sign-off before Phase 5 build starts | FUTURESTATE §11.2 |
| C2 | Every cutover requires staging soak + rollback drill — no exceptions | IMPLEMENTATION working agreements |

## 2. Team Topology (target from M4, grown from M1)

| Squad | Who | Owns |
|---|---|---|
| **Platform squad** (2 eng, 0.5 DevOps) | Eng A (lead), Eng B | Phases 0-1 deliverables: mesh, Kafka, CI/CD, identity, gateway, audit, notification, search + CDC bridge platform (P2-F2) |
| **Domain stream 1** (2 eng) | Eng C (lead), Eng D | party → product/inventory → order → billing/finance (Phases 2-5 critical path) |
| **Domain stream 2** (2 eng) | Eng E, Eng F | Long-tail domains (Phase 6 tracks, starting after party template exists) + SPA |
| **SME (part-time)** | accounting expert | Entity split review (P5 precondition), trial-balance verification |
| **EM/PM (you)** | — | Gate reviews, prioritization, unblocking |

RACI (condensed): squads **R**esponsible for stories; stream leads **A**ccountable for phase gates; SME **C**onsulted on accounting data; all **I**nformed via gate reviews.

## 3. Timeline (16 months to decommission-ready)

| Months | Phase | Staffing focus | Milestone exit |
|---|---|---|---|
| M1-M2 | **0 Infrastructure** | Platform 100%, Stream 1 on repo scaffold + ArchUnit conventions | CG-0 @ M2 end |
| M2-M4 | **1 Platform services** | Platform 70% (identity/gateway), Stream 1 starts party domain 30% | CG-1 @ M4 end |
| M4-M6 | **2 party + CDC bridge** | Platform 50% (CDC bridge = platform asset), Stream 1 50% party | CG-2 @ M6 end — **first production cutover** |
| M6-M8 | **3 product + inventory** | Stream 1 100%; Stream 2 finishes SPA skeleton, starts P6 tracks | CG-3 @ M8 |
| M8-M10 | **4 order** | Stream 1 100%; Stream 2 P6 tracks (T2 workeffort, T4 content) | CG-4 @ M10 — 30-day soak → prod @ M11 |
| M10-M13 | **5 billing + finance** | Stream 1 + SME heavy; Stream 2 finishes T3 humanres, T1 manufacturing | CG-5 @ M13 |
| M11-M14 | **6 long tail completes** | Stream 2 100% (T5 marketing last); Platform hardening, runbooks | CG-6 (all tracks) @ M14 |
| M14-M16 | **7 decommission** | Both streams: bridge removal, archival, ops handover; 30-day zero-rollback watch | CG-7 @ M16 |

**Critical path:** CG-0 → CG-1 → CG-2 (party is template + CDC foundation) → CG-3 → CG-4 → CG-5 → CG-7. Phase 6 floats off-path after M6.

**Buffer:** ~1 month unallocated (M15-M16 overlap absorbs slippage). If CG-2 slips past M6.5, re-baseline: pull Stream 2 onto CDC bridge to compress, delay Phase-6 start.

## 4. Milestones (demo-able, not just gates)

| ID | Month | Demo | Gate ref |
|---|---|---|---|
| M-0 | M2 | Hello-world service through full pipeline, mTLS-only, dashboards live | CG-0 |
| M-1 | M4 | OIDC login → gateway → demo route; monolith reachable behind gateway | CG-1 |
| M-2 | M6 | Party search in SPA on new stack; consistency delta 0 report; rollback drill recorded | CG-2 |
| M-3 | M8 | Browse catalog + check stock in SPA; 100-parallel-reserve oversell test passes | CG-3 |
| M-4 | M10 | Place order end-to-end via SPA → sagas fire (reserve, attribution) | CG-4 |
| M-5 | M13 | Invoice → GL posting → trial balance equals monolith | CG-5 |
| M-6 | M14 | All 12 domains on target stack in staging | CG-6 |
| M-7 | M16 | Monolith scaled to zero; ops handover signed | CG-7 |

## 5. Risk Register (reviewed at every gate)

| ID | Risk | L | I | Mitigation | Trigger → response |
|---|---|---|---|---|---|
| R1 | Accounting entity split contested by SME | M | H | Draft split exists (FUTURESTATE-LOWLEVEL §2.1); review scheduled M4, decision by M8 hard stop | M8 no sign-off → descope Phase 5 to billing-only, finance follows later |
| R2 | CDC reverse-sync data drift in production | M | H | Consistency checker runs continuously post-cutover; alert on delta | any drift >0 → freeze writes on slice, reconcile, root-cause before unfreeze |
| R3 | Kafka/mesh operational learning curve | H | M | Phase 0 dedicated to platform; DevOps 0.5 from M1; runbooks as gate items | CG-0 missed by >2 weeks → extend one month, compress Phase 1 scope (search last) |
| R4 | Monolith in-flight feature work conflicts with frozen slices | M | M | Cutover calendar published; per-slice write freeze only during flip window | conflicting feature lands → delay that slice's flip, not the phase |
| R5 | Key-person risk (CDC bridge, identity) | M | H | Pair rotation across platform assets; bridge is documented platform module, not tribal knowledge | single-owner asset → mandatory pair for 2 sprints |
| R6 | Performance regression vs monolith (entity caches) | M | M | p99 budgets in story ACs; load test at each CG; Caffeine+Redis per FUTURESTATE | p99 breach at CG → perf spike before next phase start |
| R7 | SPA build under-resourced (Stream 2 side-quest) | M | M | SPA skeleton is CG-2 exit; screens are story ACs per domain | CG-2 slips on SPA → contract UI help or defer screens one phase |
| R8 | Scope creep (modernize while migrating) | H | M | IMPLEMENTATION.md is authority; changes require re-baseline PR + timeline impact | any gate slippage → explicit re-baseline decision, never silent |
| R9 | Rollback needed after production cutover | L | H | Rollback drill is cutover precondition; 30-day soak on order phase | rollback executed → incident review + CDC reverse integrity check before retry |
| R10 | Team attrition mid-program | L | H | Docs-first discipline (this doc set) means any engineer can onboard from repo | attrition → pause new phase starts until backfilled + 2-week ramp |

## 6. Governance & Cadence

- **Daily:** squad standups (async OK); blockers → EM same day.
- **Bi-weekly:** cross-squad sync (contract changes, CDC platform needs).
- **Per phase gate:** gate review meeting — walk CG checklist with evidence links; go/no-go recorded in `issues/P<n>-GATE.md` (check boxes committed); demo recorded.
- **Change control:** scope/story changes = PR against IMPLEMENTATION.md + this file in same PR (working agreement #3); timeline re-baseline only at gate reviews.
- **Definition of Done:** IMPLEMENTATION.md "Cross-Phase Working Agreements" — unchanged here.

## 7. Success Metrics

| Metric | Target | Measured |
|---|---|---|
| Phases exited on gate | 8/8 via CG checklists, zero silent slippage | gate review minutes |
| Cutover integrity | consistency delta = 0 at every cutover + zero data-loss rollbacks | checker job history |
| Rollback drills executed | 1+ per domain cutover, recorded | drill artifacts |
| Service p99 | < 200ms sync internal calls in staging load tests | Grafana at each CG |
| Test coverage | domain modules > 90% unit (story AC) | CI reports |
| Soak stability | 30-day windows with zero sev-1 on new stack (phases 4, 7) | incident log |
| Monolith write-surface | monotonically decreasing per phase (domains frozen) | gateway route table diff |

## 8. What This Plan Does Not Cover

- Vendor/cloud selection and cost (Phase 0 produces the cost model as deliverable).
- Hiring/contracting plan (assumes A1 staffing exists at M1).
- Monolith feature roadmap during migration (owned separately; R4 governs interaction).

---

*v1, 2026-10-16. Re-baseline only at gate reviews. Sources: IMPLEMENTATION.md (stories/gates), FUTURESTATE-ARCHITECTURE.md §10-11 (open questions tracked as risks R1, R7).*
