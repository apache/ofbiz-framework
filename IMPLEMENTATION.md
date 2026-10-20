# IMPLEMENTATION.md — Migration Delivery Plan (Monolith → Microservices)

> **Authoritative roadmap** elaborating [FUTURESTATE-ARCHITECTURE.md](FUTURESTATE-ARCHITECTURE.md) §10 and [FUTURESTATE-LOWLEVEL.md](FUTURESTATE-LOWLEVEL.md). Where this document and FUTURESTATE §10 differ on phase composition, **this document wins** (it refines Phase 0 into infrastructure-only and adds a dedicated platform-services phase).
> **Hierarchy:** `Phase → Feature (F) → Epic (E) → Story (S) → Task`. Every story carries acceptance criteria (AC); every epic and phase carries a completion gate (CG). ACs are verifiable by command, inspection, or test — no "works correctly".
> **ID scheme:** `P<phase>-F<n>-E<n>-S<n>` — e.g. `P2-F1-E1-S1` (Phase 2, Feature 1, Epic 1, Story 1).
> **Estimates:** S (<1d) / M (1-3d) / L (1w+) per story, single-engineer basis.

---

## Phase Overview

| Phase | Scope | Depends on |
|---|---|---|
| 0 | Infrastructure: cluster, mesh, Kafka, registry, observability, CI/CD, repo scaffolding | — |
| 1 | Platform services: identity, gateway, audit, notification, search | 0 |
| 2 | party-svc + CDC bridge foundation (first strangler cut) | 1 |
| 3 | product-svc, inventory-svc, search wiring | 2 |
| 4 | order-svc | 3 |
| 5 | billing-svc, finance-svc (accounting split) | 4 |
| 6 | Long tail: manufacturing, workeffort, humanres, content, marketing (parallelizable) | 2 (party) — independent of 3-5 |
| 7 | Decommission monolith | 1-6 all |

---

# PHASE 0 — Infrastructure Build

**Objective:** everything later phases deploy onto exists and is provable: cluster, mesh, Kafka + schema registry, observability stack, CI/CD, and the repository scaffolding/conventions from FUTURESTATE-LOWLEVEL §1.

**Entry criteria:** cloud account/cluster admin access; DNS wildcard delegated; this repo at current trunk.

**Phase completion gate (CG-0):** every item below checked with evidence links:
- [x] `kubectl get pods -A` green on cluster; mesh-injected namespace `ofbiz` exists (local kind: all pods Ready; `platform/bootstrap/10-verify-mtls.sh` → ALL CHECKS PASSED)
- [x] Kafka (Strimzi) + Schema Registry reachable from a test pod; topic create/consume roundtrip proven (`platform/kafka/20-verify.sh` roundtrip PASS, `21-verify-broker-kill.sh` 100/100 no loss, schema-registry deploy in `platform/kafka/10-install.sh`)
- [ ] OTel collector receiving spans/metrics from a demo deployment; Jaeger trace and Grafana dashboard visible (local profile stack verified earlier — see P0-F3; re-run `platform/observability/20-verify.sh` for current evidence)
- [ ] CI pipeline builds a hello-world Spring Boot 3.3 service (convention plugin) and pushes an image to registry; CD deploys it via the shared Helm chart with mTLS enforced (push step added to `.github/workflows/ci.yml` — evidence = next green trunk run)
- [x] Repo scaffold: `buildSrc` convention plugins compile; ArchUnit layering test enforces domain-purity on a sample module (P0-F4: `./gradlew build` green, red-green evidence in `DemoConformanceTest`)

## P0-F1 Cluster & Service Mesh
**Epic: provision K8s + Istio (CG: cluster ready, mesh strict mTLS)**
- **S1 (M):** Provision cluster, `ofbiz` namespace, Istio with `PeerAuth: STRICT`. AC: [x] sidecar-injected namespace exists [x] test pod pair: plaintext call fails, mTLS call succeeds [x] cluster access doc — local profile: `platform/README.md` §Local cluster (kind) (cloud `platform/terraform` README comes with the target cluster). Evidence: `platform/bootstrap/10-verify-mtls.sh` → ALL CHECKS PASSED.
- **S2 (S):** cert-manager + wildcard DNS + gateway LB. AC (local profile): [x] `https://ofbiz.127.0.0.1.sslip.io` resolves to istio-ingress, serves CA-verified wildcard cert (`platform/certmanager/20-verify.sh` → HTTP 200 `{"status":"UP"}` from demo-svc, chain verified against ofbiz-root-ca) [x] auto-renewal scheduled (duration 1h / renewBefore 45m, `renewalTime` verified). Cloud deltas: Let's Encrypt staging issuer + delegated wildcard DNS + LB (no NodePort/hostPort) — annotated in `platform/certmanager/10-install.sh`.
- **S3 (S):** Vault CSI + K8s secrets baseline. AC: [ ] test secret mounted into pod [ ] rotation doc. (local profile: `platform/vault/10-install.sh` + `20-verify.sh`; rotation doc = `platform/vault/README.md`)

## P0-F2 Event Backbone
**Epic: Kafka + Schema Registry (CG: event plane proven)**
- **S1 (M):** Strimzi Kafka cluster (3 brokers, KRaft) + Apicurio Schema Registry. AC: [x] topic create from ops pod [x] producer/consumer roundtrip passes (`platform/kafka/20-verify.sh`) [x] one-broker kill causes no data loss (`platform/kafka/21-verify-broker-kill.sh`: 100/100 messages, 0 dups).
- **S2 (S):** Topic namespace + ACL conventions script (`platform/helm/topics/10-create.sh`). AC: [x] script creates `<context>.<aggregate>.<action>.v1` topics per FUTURESTATE §8 [x] DLQ created for test topic [~] ACLs restrict produce/consume per service principal — DECLARED (least-privilege KafkaUser manifests, principals CN=order-svc/notification-svc/audit-svc) but broker-side simple authz DEFERRED: Strimzi 1.3.0 / Kafka 4.3.1 combined nodes ignored `super.users` and produced per-user produce checks contradicting the declared ACLs. Enforcement evidence lands on the cloud profile (dedicated controllers); see note in `platform/helm/topics/10-create.sh` and `kafka-cluster.yaml`.
- **S3 (M):** Avro contract pipeline. AC: [ ] schema registered via CI for sample `order.v1.OrderPlaced` (contracts job in `.github/workflows/ci.yml`, Apicurio service container; evidence = next trunk run) [ ] BACKWARD compatibility gate rejects an incompatible change in a test PR (409 assertion in the same job) [x] artifact published to contracts path (`contracts/order/v1/OrderPlaced.avsc`).

## P0-F3 Observability (EXPANDED — observability from inception)
**Epic: full-stack observability (CG: all five stores verified) — DONE for local profile**
- **S1 (M):** Jaeger (OTLP+zipkin) + Istio mesh tracing wiring. AC: [x] Jaeger ingests OTLP traces (verified) [ ] mesh-span emission — KNOWN-ISSUE-1 on Istio 1.31 minimal profile; per-service OTel SDK is the primary contract.
- **S2 (M):** Prometheus + Grafana (4 datasources). AC: [x] targets scraped, datasources provisioned (verified).
- **S3 (M):** Loki + promtail log aggregation. AC: [x] ofbiz container logs queryable (verified).
- **S4 (M):** OpenSearch + fluent-bit long-retention log analytics. AC: [x] `ofbiz-logs` index live with documents (verified).
Evidence: `platform/observability/20-verify.sh` → ALL CHECKS PASSED.

## P0-F5 Local Object Storage (S3 contract)
- **S1 (M):** Garage (local S3) + program buckets (ADR-008: MinIO images purged 2025). AC: [x] buckets ofbiz-content/ofbiz-audit [x] S3 put/get roundtrip via awscli (verified). Evidence: `platform/storage/20-verify.sh` → ALL CHECKS PASSED.

## P0-F4 CI/CD & Scaffolding
**Epic: build conventions (CG: hello-world service shipped through full pipeline)**
- **S1 (L):** `buildSrc` convention plugins (`java-library`, `spring-boot-service`, `grpc`, `contracts-test`). AC: [x] plugins applied to `platform/demo-svc` compile [x] gRPC codegen produces stubs [x] Boot 3.3 image built [x] scanned — trivy HIGH/CRITICAL gate enforced in CI (`.github/workflows/ci.yml`) [ ] pushed — GHCR push step added to CI (trivy-action bumped to v0.36.0 after the 0.28.0 resolve failure in run 39430486272); evidence = next green trunk run.
- **S2 (M):** Shared Helm `service-chart`. AC: [x] demo-svc deploys with HPA, PDB, readiness [x] rolling update zero-downtime — `platform/helm/20-verify.sh`: 44,665 requests, 0 failures during upgrade.
- **S3 (M):** Pipeline: build → test → contract-publish → image → staging deploy. AC: [ ] green run on demo PR (pipeline in `.github/workflows/ci.yml`; evidence pending first push) [ ] broken test blocks merge (evidence: red run link pending).
- **S4 (S):** ArchUnit layering rule in conventions. AC: [x] rule fails on a sample violation (`DemoConformanceTest.violationIsRejected`) [x] passes on clean module.

---

# PHASE 1 — Platform (Common) Services

**Objective:** the five cross-cutting services stand up skeleton-to-working on Phase-0 infra. No domain migration yet.

**CG-1:**
- [ ] OIDC login flow issues a JWT usable against a gateway demo route
- [ ] gateway routes to demo-svc with JWT validation + rate limit proven (429 on burst)
- [ ] audit-svc consumes a demo topic wildcard and stores events queryably
- [ ] notification-svc sends a test email on `notification.requested.v1`
- [ ] search-svc indexes a demo document and answers a query via API
- [ ] All 5 services: CI green, dashboards live, mTLS-only, DLQ wired

## P1-F1 identity-svc
**Epic: OIDC issuer (CG: external token accepted by demo route)**
- **S1 (L):** Spring Authorization Server: auth-code+PKCE, client-credentials, JWKS endpoint. AC: [ ] OIDC discovery doc served [ ] token for test client validates at gateway [ ] refresh flow works [ ] password grant absent (security check).
- **S2 (M):** Credential store + Flyway schema + user CRUD admin API. AC: [ ] create/verify user roundtrip [ ] Shiro-compatible hashing [ ] audit event emitted on login.
- **S3 (M):** Party-link projection consumer (stub for Phase 2). AC: [ ] consumes `party.party.*` into local projection table [ ] idempotent replay proven (restart, zero duplicates).

## P1-F2 gateway
**Epic: edge (CG: routing+authn+ratelimit)**
- **S1 (M):** Spring Cloud Gateway + JWKS JWT filter + route table. AC: [ ] `/api/v1/demo/**` → demo-svc 200 [ ] expired/absent token → 401 [ ] unknown context → 404 not 5xx.
- **S2 (S):** Rate limit (Redis). AC: [ ] burst over limit → 429 with Retry-After.
- **S3 (M):** Strangler default route → monolith (staging monolith via docker image + JWT→header bridge). AC: [ ] `https://<domain>/webtools/...` proxies to monolith [ ] host-header config generated for monolith [ ] trace spans gateway→monolith.
- **S4 (S):** CORS + security headers. AC: [ ] preflight OK for SPA origin [ ] HSTS/CSP headers present.

## P1-F3 audit-svc
- **S1 (M):** Wildcard consumer + partitioned Postgres store. AC: [ ] events from 3 test topics persisted in order [ ] monthly partition created [ ] query API returns by time+topic with pagination.
- **S2 (S):** Secret-audit event schema + storage. AC: [ ] test secret event stored + alert on high severity.

## P1-F4 notification-svc
- **S1 (M):** `notification.requested.v1` consumer + SMTP adapter + templates. AC: [ ] test command → email received (mailhog staging) [ ] retry then DLQ on failure injection [ ] template render with locale.

## P1-F5 search-svc
- **S1 (L):** OpenSearch + consumer framework + query API. AC: [ ] index doc via event [ ] gRPC + REST query returns hit [ ] reindex-from-topic command rebuilds index.

---

# PHASE 2 — party-svc + CDC Bridge

**Objective:** first domain migrated; CDC interop pattern proven end-to-end (the template for all later phases).

**CG-2:**
- [ ] party-svc owns party schema (133 entities → JPA subset for live slices; historical archive documented)
- [ ] Debezium streams monolith party tables → `ofbiz.party.*` topics (lag < 5s at 100 msg/s test)
- [ ] Write cutover done in staging: gateway routes `/api/v1/parties/**` to party-svc; reverse-sync keeps monolith tables current; consistency job delta = 0 on a 10k-party dataset
- [ ] Rollback drill executed: route flip back, zero data loss
- [ ] SPA (skeleton) lists/searches parties via new API

## P2-F1 party-svc domain
**Epic: core aggregate (CG: party CRUD via API)**
- **S1 (L):** Clean-arch scaffold + Party aggregate (person/group, roles, relationships). AC: [ ] ArchUnit green [ ] upsert/fetch via gRPC+REST [ ] outbox emits `party.party.created/updated` [ ] domain unit tests >90% on aggregate.
- **S2 (M):** ContactMech + purposes. AC: [ ] link/unlink email/phone/address with purpose validation [ ] event emitted [ ] contract test published.
- **S3 (M):** FindPartiesByRole query + pagination. AC: [ ] p99 < 200ms at 10k rows seeded [ ] OpenAPI pagination envelope matches FUTURESTATE §8 shape.
## P2-F2 CDC Bridge (reusable platform)
**Epic: monolith interop (CG: bridge reusable by later phases)**
- **S1 (L):** Debezium connector config + `ofbiz.<entity>.<action>` topics + Avro schemas. AC: [ ] insert/update/delete on monolith party table → events within 5s [ ] snapshot completes on party slice.
- **S2 (M):** Projection builder library (platform module). AC: [ ] party-svc projection table rebuilt from scratch via re-consume [ ] idempotent.
- **S3 (M):** Reverse-sync adapter. AC: [ ] `party.party.updated` applies back to monolith table [ ] conflict policy documented (new-service wins).
- **S4 (M):** Consistency checker job. AC: [ ] delta report (row counts + field checksums) [ ] non-zero exit on drift (CI-gateable).
## P2-F3 identity integration
- **S1 (M):** JWT claims from party projection (roles→permissions). AC: [ ] token carries party roles [ ] gateway 403 on missing role [ ] revocation on `party.role.revoked` within 5s.

---

# PHASE 3 — product-svc + inventory-svc

**CG-3:**
- [ ] Catalog browse + price lookup via new APIs in SPA
- [ ] Stock reserve/release saga works; oversell impossible under concurrency (100 parallel reserves, 0 over-reserved)
- [ ] search-svc indexes products on change events; search p99 < 100ms @ 100k docs
- [ ] CDC cutover + consistency delta 0 for product/shipment slices; rollback drill done

## P3-F1 product-svc
- **S1 (L):** Product aggregate + categories + associations (≈192 entities; mapping table checked in). AC: [ ] CRUD via API [ ] category tree query [ ] events emitted [ ] ArchUnit + unit tests.
- **S2 (M):** Pricing (price rules, currencies). AC: [ ] price resolution respects date-range rules [ ] `product.price.changed` on update [ ] snapshot-at-order contract test.
- **S3 (S):** ProductStore/config exposure. AC: [ ] store settings API read/write.
## P3-F2 inventory-svc
- **S1 (L):** Facility + InventoryItem + stock ops (facility slice ≈17 + shipment 47 entities). AC: [ ] adjust/transfer/receive flows [ ] events emitted.
- **S2 (L):** Reservation saga (reserve→confirm/cancel). AC: [ ] optimistic-lock concurrency test: parallel reserves never oversell [ ] compensation on cancel [ ] p99 reserve < 200ms.
- **S3 (M):** Shipment creation/packing/status. AC: [ ] ship-order flow transitions statuses [ ] `inventory.stock.shipped` emitted.
## P3-F3 search wiring
- **S1 (M):** product indexer consumer. AC: [ ] product create/update → searchable < 5s [ ] field mapping versioned.

---

# PHASE 4 — order-svc

**CG-4:**
- [ ] Full order lifecycle (quote→order→approve→complete/cancel) via new stack in staging e2e test
- [ ] ECA-derived sagas: inventory reserve, invoice trigger, marketing attribution all fire on `order.order.placed`
- [ ] Monolith order writes disabled in staging; 30-day soak before prod cutover
- [ ] Consistency delta 0 vs monolith order tables; rollback drill done

## P4-F1 order domain
- **S1 (L):** Order aggregate + status machine (enum-guarded transitions). AC: [ ] illegal transition rejected with typed error [ ] history appended per transition [ ] events per state change.
- **S2 (M):** Cart → order placement (price snapshot from product API/events). AC: [ ] order stores immutable price snapshot [ ] stock check via inventory reservation.
- **S3 (M):** Returns + adjustments. AC: [ ] return creation adjusts inventory (saga) [ ] credit memo event emitted.
- **S4 (S):** Quotes. AC: [ ] quote→order conversion preserves line data.
## P4-F2 integrations (sagas)
- **S1 (M):** placeOrder saga orchestrator (reserve stock, validate party, emit placed). AC: [ ] compensation on partial failure [ ] saga state persisted + resumable after restart.
- **S2 (S):** Marketing attribution consumer. AC: [ ] tracking-code attribution recorded on placed orders.

---

# PHASE 5 — billing-svc + finance-svc

**CG-5:**
- [ ] SME-approved entity split (FUTURESTATE-LOWLEVEL §2.1) frozen in a migration mapping doc
- [ ] Invoice issue→settle e2e; payment gateway adapter (test gateway) auth/capture/void
- [ ] GL auto-posting from billing events; trial balance equals monolith trial balance on migrated dataset
- [ ] notification-svc sends invoice emails; PCI scope documented as confined to billing-svc

## P5-F1 billing-svc
- **S1 (L):** Invoice aggregate + issue/post/settle. AC: [ ] lifecycle states guarded [ ] `billing.invoice.*` events [ ] PDF render adapter.
- **S2 (L):** Payment aggregate + gateway adapter framework. AC: [ ] auth/capture/void/retry against test gateway [ ] idempotency keys [ ] DLQ on outage with reconciliation report.
- **S3 (M):** BillingAccount + terms. AC: [ ] credit-limit check on invoice.
## P5-F2 finance-svc
- **S1 (L):** GL: accounts, AcctgTrans postings, periods. AC: [ ] double-entry invariant enforced [ ] closed period blocks postings.
- **S2 (M):** Auto-posting rules consumer (`billing.invoice.issued/settled` → AcctgTrans). AC: [ ] rules configurable per account mapping [ ] failure → parked-for-review, never dropped.
- **S3 (M):** Trial-balance report API. AC: [ ] matches monolith trial balance on identical dataset (checker evidence).
## P5-F3 consistency
- **S1 (M):** Accounting migration mapping doc + checker for both new DBs. AC: [ ] delta 0 run in staging.

---

# PHASE 6 — Long-Tail Domains (5 parallel tracks)

Each track reuses the Phase-2 CDC template. Per-track CG = Phase-2 pattern (own domain, cutover, delta 0, rollback drill).

- **P6-T1 manufacturing-svc:** BOM + routing + MRP-as-Streams (demand ← order events; supply ← inventory). CG: [ ] MRP over test demand produces proposed orders identical to monolith MrpServices on the same dataset.
- **P6-T2 workeffort-svc:** projects/tasks/timesheets + iCal export. CG: [ ] iCal feed renders in an external calendar.
- **P6-T3 humanres-svc:** positions/employments consuming party events. CG: [ ] onboarding saga creates party role + employment.
- **P6-T4 content-svc:** content trees + S3-compatible object storage. CG: [ ] upload→publish→URL fetch roundtrip.
- **P6-T5 marketing-svc:** campaigns/tracking/opportunities. CG: [ ] attribution report joins order events by tracking code.

## Shared stories (each track)
- **S1 (L):** domain aggregate per entitymodel slice. AC: ArchUnit, CRUD API, events, unit tests — same contract as P2-F1-S1.
- **S2 (M):** CDC + reverse-sync + consistency checker instantiation. AC: delta 0 in staging; rollback drill.
- **S3 (S):** SPA screens for the domain. AC: [ ] browser-verified CRUD on primary entity.

---

# PHASE 7 — Decommission

**CG-7 (final):**
- [ ] All 12 domains served by target stack in production ≥ 30 days
- [ ] CDC bridges removed; reverse-sync adapters deleted; `ofbiz.*` topics archived
- [ ] Monolith scaled to zero 30 days with zero rollback events, then archived (image + DB snapshot retained per policy)
- [ ] Post-monolith ops runbook (service ownership) signed off

## Stories
- **S1 (M):** Route cleanup + bridge removal. AC: [ ] no gateway route references monolith [ ] Kafka shows zero `ofbiz.*` traffic for 7 days.
- **S2 (S):** Data archival. AC: [ ] final monolith DB snapshot + checksum manifest stored [ ] restore tested once.
- **S3 (M):** Ops handover. AC: [ ] service ownership matrix [ ] on-call routing updated.

---

## Cross-Phase Working Agreements

1. **Definition of Done (every story):** AC met with evidence link, CI green, contracts backward-compatible, dashboards updated, docs updated (this file + service README).
2. **Every cutover:** staging first → consistency delta 0 → rollback drill → production flip in change window.
3. **Doc alignment:** scope changes update FUTURESTATE-ARCHITECTURE §5/§6 + this file in the same PR.
4. **Estimates are planning aids; gates are the truth.** A phase exits on its CG checklist, not the calendar.

---

*Delivery plan v1, 2026-10-16. Refines FUTURESTATE-ARCHITECTURE.md §10 (Phase 0 = infrastructure only; platform services moved to Phase 1). FUTURESTATE §10 remains the strategic summary; this file is the execution authority.*
