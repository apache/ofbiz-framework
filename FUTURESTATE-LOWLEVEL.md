# FUTURESTATE-LOWLEVEL.md — Target-State Implementation Reference

> Companion to [FUTURESTATE-ARCHITECTURE.md](FUTURESTATE-ARCHITECTURE.md) (target design decisions), [ARCHITECTURE.md](ARCHITECTURE.md)/[LOWLEVEL.md](LOWLEVEL.md) (current monolith).
> This is the "how it's actually built" reference for the target state: scaffolding, schemas, DDL, deployment shapes, and OFBiz→target mapping. Nothing here is built yet — this is the blueprint the per-service specs (`/prd-to-spec`) and implementation plans are written against.

---

## 1. Repository & Module Scaffolding

**Mono-repo layout (default pending Open Question §11.1):**

```
ofbiz-msa/
  platform/                      # shared infra, not shared business code
    helm/                        # umbrella chart: kafka, registry, mesh, obs
    terraform/                   # cluster + cloud resources
    contracts/                   # published API/event schemas (see §4)
  services/
    party-svc/
      party-domain/              # pure Java 21, no Spring/Framework deps
      party-application/
      party-adapters/
        adapters-grpc/
        adapters-rest/
        adapters-kafka/
      party-infrastructure/
      Dockerfile                 # distroless, JRE 21
    product-svc/ ...             # same shape ×11 domain + ×5 platform
  spa/                           # Angular 17+ workspace (Nx)
  docs/
```

**Parent conventions (every service):**
- Java 21, Spring Boot 3.3.x, Gradle Kotlin DSL (Kotlin for build scripts only unless a service opts in).
- `buildSrc` convention plugins: `ofbiz-msa.java-library` (domain), `ofbiz-msa.spring-boot-service` (adapters/infra), `ofbiz-msa.grpc` (protobuf codegen), `ofbiz-msa.contracts-test` (contract verification).
- Virtual threads: `spring.threads.virtual.enabled=true`; structured concurrency in sagas.
- Dependencies *only* inward: `adapters|infrastructure → application → domain`. Enforced by ArchUnit test in each module.

**Platform BOM pins:** Boot 3.3.x, gRPC 1.6x, kafka-clients 3.7.x, Postgres 16 driver, OTel 2.x, Flyway 10, Testcontainers 1.19.

## 2. Entity Ownership Assignment (904 entities)

Base rule: entitymodel file → owning service. Two splits drafted here (**needs SME review — FUTURESTATE-ARCHITECTURE.md §11.2**).

### 2.1 accounting-entitymodel.xml (184) → billing-svc / finance-svc (draft split by prefix)

| Prefix group | Entities (n) | Assigned to | Rationale |
|---|---|---|---|
| `Payment*` | 41 | billing-svc | payment processing + gateways |
| `BillingAccount*`, `Budget*` (B) | 23 | billing-svc | AR documents |
| `Invoice*` (I) | 27 | billing-svc | invoice documents |
| accounting residuals (R) | 3 | billing-svc | refund docs |
| `FinAccount*`, `FiscalGlActivity*` (F) | 34 | finance-svc | ledger accounts |
| `GlAccount*`, `GlJournal*` (G) | 26 | finance-svc | GL core |
| `AcctgTrans*` (A) | 12 | finance-svc | journal entries |
| `TaxAuthority*`, `Term*` (T) | 10 | finance-svc | tax/terms config |

Totals: billing ≈ 94, finance ≈ 90. Crossover entities resolved per-entity in the billing/finance migration spec.

### 2.2 product-entitymodel.xml (209) → product-svc / inventory-svc

| Prefix group | Goes to |
|---|---|
| `Product*`, `ProdCatalog*`, `ProductPrice*`, `ProductFeature*`, `ProductCategory*`, `ProductConfig*`, `ProductStore*` | product-svc |
| `Facility*`, `InventoryItem*`, `Container*`, `Lot*`, `Reorder*` | inventory-svc |

### 2.3 Remaining files (unsplit)

party(133)→party-svc · order(114)→order-svc · shipment(47)→inventory-svc · content(79)→content-svc · workeffort(41)→workeffort-svc · humanres(46)→humanres-svc · marketing(42)→marketing-svc · manufacturing(9)→manufacturing-svc.

**Cross-context references** (e.g. `OrderItem.productId`) become ID columns with no FK across services; referential integrity enforced by consuming validation events, not joins.

## 3. gRPC Contract Sketches

Proto files live in `platform/contracts/proto/<context>/v1/<context>.proto`, compiled artifact published per service. Representative surface (order-svc shown; others follow the same shape — full field lists belong to each service's spec):

```protobuf
syntax = "proto3";
package order.v1;

service OrderService {
  rpc PlaceOrder(PlaceOrderRequest) returns (PlaceOrderResponse);
  rpc GetOrder(GetOrderRequest) returns (Order);
  rpc TransitionStatus(TransitionRequest) returns (Order);
}

message Order {
  string order_id = 1;
  string order_name = 2;
  PartyRef bill_to_party = 3;      // party-svc reference, by ID
  repeated OrderItem items = 4;
  OrderStatus status = 5;
  int64 placed_at_epoch_ms = 6;
}
```

- Error model: `google.rpc.Status` details; domain errors as typed `ErrorInfo` reasons (`order.ORDER_INVALID_STATE` etc.).
- Deadlines propagate from gateway; per-call OTel `traceparent` metadata.
- REST edge mirrors the same operations (OpenAPI generated from proto where possible).

## 4. Event Schemas & Topic Inventory

**Avro in `platform/contracts/avro/<context>/v1/`:**

```json
{"type": "record", "name": "OrderPlaced", "namespace": "order.v1",
 "fields": [
   {"name": "event_id", "type": "string"},
   {"name": "order_id", "type": "string"},
   {"name": "items", "type": {"type": "array", "items": "order.v1.ItemRef"}},
   {"name": "occurred_at", "type": {"type": "long", "logicalType": "timestamp-millis"}}]}
```

**Topic inventory (initial):**

| Topic | Producer | Consumers |
|---|---|---|
| `party.party.created.v1` / `.updated.v1` | party-svc | identity-svc, order-svc, hr, marketing, search, audit |
| `product.product.updated.v1`, `product.price.changed.v1` | product-svc | order-svc, inventory-svc, search, audit |
| `order.order.placed.v1` … | order-svc | inventory-svc, billing-svc, manufacturing-svc, marketing-svc, audit |
| `inventory.stock.reserved.v1` / `.adjusted.v1` / `.shipped.v1` | inventory-svc | order-svc, manufacturing-svc, audit |
| `billing.invoice.issued.v1` / `.settled.v1`, `billing.payment.captured.v1` | billing-svc | finance-svc, notification-svc, audit |
| `finance.transaction.posted.v1`, `finance.period.closed.v1` | finance-svc | billing-svc, audit |
| `notification.requested.v1` (command) | any | notification-svc |
| `manufacturing.mrp.demand.v1`, `.supply.v1` (streams) | manufacturing-svc | manufacturing Streams job, inventory-svc |
| `ofbiz.<entity>.<action>` (CDC bridge, transient) | Debezium | migrating services' projections (per phase) |

## 5. Outbox Pattern — Mechanics & DDL

Every service schema (Flyway baseline `V1__outbox.sql`):

```sql
CREATE TABLE outbox (
  event_id        UUID PRIMARY KEY,
  aggregate_type  TEXT NOT NULL,
  aggregate_id    TEXT NOT NULL,
  event_type      TEXT NOT NULL,          -- e.g. order.order.placed
  schema_version  INT  NOT NULL DEFAULT 1,
  payload         JSONB NOT NULL,
  headers         JSONB NOT NULL DEFAULT '{}',
  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  published_at    TIMESTAMPTZ NULL
);
CREATE INDEX outbox_unsent ON outbox (created_at) WHERE published_at IS NULL;
```

- Relay: in-process poller (2s) using `FOR UPDATE SKIP LOCKED` → publish Avro → set `published_at`. Debezium reserved for the monolith CDC bridge only.
- Consumer idempotency: `processed_events(event_id PK)` per consumer group; insert-or-skip before handling.
- Ordering: Kafka key = aggregate_id.

## 6. Deployment Shape (K8s + Mesh)

```
platform/helm/
  umbrella/           # kafka (Strimzi), schema-registry, cert-manager,
                      # otel-collector, jaeger, prometheus, grafana, redis
  service-chart/      # generic chart every -svc reuses
                      # image, grpc 9090, http 8080, HPA (CPU+latency), PDB,
                      # readiness /actuator/health/readiness
```

- Mesh: Istio — `PeerAuth: STRICT` mTLS; `DestinationRule` retries: 2 attempts, 200ms connect / 1s route timeouts; `AuthorizationPolicy` per service: only gateway + declared callers.
- Ingress TLS at gateway; wildcard cert via cert-manager.
- Config: Spring profile `k8s`; secrets via Vault CSI.
- Strangler routes (gateway, ordered): migrated contexts first, fallback `/** → ofbiz-monolith.ofbiz.svc:8443` with JWT→header bridge.

## 7. CDC Bridge (Monolith Interop)

- Debezium Postgres connector on monolith DB → `ofbiz.<entity>.<action>` topics (Avro, schema-per-table).
- Migrating service (phase N) builds **projection tables** in its own DB from relevant CDC topics (e.g. order-svc projects minimal party rows it needs during transition).
- Write cutover: gateway flips route; new service emits domain events; a **reverse adapter** (anti-corruption layer, `adapters-kafka/reverse-sync`) writes back to monolith tables for not-yet-migrated consumers until their phase completes.
- Per-phase consistency job: row counts + checksums old vs new (report only, no auto-repair).

## 8. OFBiz → Target Mapping Table

| OFBiz mechanism (current, LOWLEVEL.md ref) | Target replacement |
|---|---|
| `ControlServlet`/`RequestHandler`/controller.xml | gateway routes + REST/gRPC adapters |
| Service Engine `runSync` (XML services) | application-layer use cases (Spring beans) |
| `EntityAutoEngine` | JPA repositories + domain factories |
| Minilang simple-methods | rewritten as Java use cases (no XML DSL carries over) |
| Service ECAs | Kafka consumers + saga orchestrators |
| `JobManager` job sandbox + semaphores | Kafka Streams / scheduled pollers + saga locks (`SKIP LOCKED`) |
| JMS listeners (`JmsListenerFactory`) | Kafka consumers |
| `LoginWorker`/`Security`/`JWTManager` | identity-svc OIDC + method security |
| Widget screens/forms/menus + themes | Angular 17+ SPA components |
| `ScreenRenderer`/FreeMarker | Angular templates; SSR optional |
| Entity caches (entityengine.xml) | Redis (edge) + Caffeine (in-service) |
| webtools admin | SPA admin module + actuator/K8s tooling |
| `SecretAuditContainer`, ServerHit | audit-svc event store |
| `ServiceXaWrapper` (XA transactions) | outbox atomicity + sagas (no XA) |
| Datafile / data import (`entityext`) | Flyway migrations + import adapters |

## 9. Local Development Setup

- `kind`/`k3d` + tilt/skaffold for hot-reloop of one service at a time.
- `docker compose` dev profile (kafka, postgres-per-service, redis, jaeger-all-in-one) for tests without cluster — mesh features only in cluster profile.
- SPA: `nx serve` + gateway stub proxy in dev.
- Test data: per-service seed scripts (derived once per phase from OFBiz demo data exports, transformed by migration tooling).

## 10. Per-Service Testing Layout

```
party-domain/src/test/            # pure unit, no containers
party-application/src/test/       # Testcontainers: postgres + kafka (outbox roundtrip)
party-adapters/grpc/src/test/     # gRPC contract tests (in-process server)
party-adapters/rest/src/test/     # MockMvc + OpenAPI validation
party-adapters/kafka/src/test/    # consumer idempotency + DLQ tests
contracts/                        # published proto + avro + OpenAPI artifacts,
                                  # consumed by other services' CI (backward-compat gates)
```

E2E per release: staging K8s, seeded data, critical journey `place order → reserve → ship → invoice → GL balance` — mirrors OFBiz demo flows.

---

*Blueprint only — every section is elaborated by the owning service's spec before any code. Entity split tables (§2) are drafts pending SME review (FUTURESTATE-ARCHITECTURE.md §11.2).*
