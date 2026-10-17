# C4 Diagrams — Monolith, Distributed Target, and Key Flows

> Sources of truth: [ARCHITECTURE.md](../../ARCHITECTURE.md) (current),
> [FUTURESTATE-ARCHITECTURE.md](../../FUTURESTATE-ARCHITECTURE.md) (target).
> Rendered by GitHub/mermaid-capable viewers. Component-level detail for the
> remaining 10 domain services follows the order-svc exemplar pattern
> (§5); per-service expansion happens in each service's spec.

---

## 1. Monolith — System Context (C4 L1)

```mermaid
C4Context
title Apache OFBiz Monolith — System Context
Person(operator, "Back-office Operator", "Order entry, invoicing, catalog, warehouse")
Person(admin, "Administrator", "Config, data tools, entity/service browsers")
Person(integrator, "API Consumer", "Headless integrations, custom frontends")
Person(dev, "Downstream Developer", "Builds custom OFBiz components")
System(ofbiz, "Apache OFBiz Monolith", "Open-source ERP: order, accounting, product, party, manufacturing, HR, content, marketing, workeffort")
SystemDb(db, "Relational Database", "One shared DB, 904 entities, 12 dialects")
System_Ext(mail, "SMTP", "Outbound email")
System_Ext(paygw, "Payment Gateways", "Authoritative external payment systems")
Rel(operator, ofbiz, "Uses", "HTTPS widget UI")
Rel(admin, ofbiz, "Uses", "HTTPS webtools")
Rel(integrator, ofbiz, "Uses", "REST/SOAP APIs")
Rel(dev, ofbiz, "Extends", "ofbiz-component.xml plugins")
Rel(ofbiz, db, "Reads/Writes", "JDBC via Entity Engine")
Rel(ofbiz, mail, "Sends email", "SMTP")
Rel(ofbiz, paygw, "Auth/Capture", "gateway protocols")
```

## 2. Monolith — Containers (C4 L2)

```mermaid
C4Container
title Apache OFBiz Monolith — Containers (single JVM)
Container(widget, "Widget UI", "FreeMarker + widget XML", "Back-office screens via 6 themes")
Container(control, "ControlServlet / RequestHandler", "Jakarta Servlet", "controller.xml routing: URI to security to event to view")
Container(rest, "REST API", "JAX-RS (org.apache.ofbiz.ws.rs)", "OpenAPI + Swagger UI; maps HTTP to services")
Container(svcengine, "Service Engine", "Java", "3,548 XML-declared services; 9 engine types; ECA, JobManager, semaphores")
Container(minilang, "Minilang", "XML DSL", "Legacy service implementations")
Container(entengine, "Entity Engine", "Java", "Delegator API; entity XML; 12 dialect fieldtypes; caches")
ContainerDb(pg, "Shared RDBMS", "Postgres/Oracle/MySQL/...", "904 entities, single database")
Rel(widget, control, "submits requests", "HTTPS")
Rel(control, svcengine, "Invokes services", "LocalDispatcher.runSync")
Rel(rest, svcengine, "Invokes services", "ServiceRequestWorker")
Rel(svcengine, entengine, "CRUD/find", "Delegator")
Rel(entengine, pg, "SQL", "JDBC")
```

## 3. Distributed Target — System Context (C4 L1)

```mermaid
C4Context
title Future State — Microservices System Context
Person(user, "Business User", "SPA end user")
Person(integrator, "API Consumer", "External integrations")
System(spa, "Angular 17+ SPA", "Back-office frontend")
System(platform, "OFBiz Microservices Platform", "16 services: 11 domain + 5 platform, K8s + Istio mTLS")
SystemDb(pgx, "Postgres per service", "DB-per-service")
SystemQueue(kafka, "Kafka", "Event backbone + outbox, Avro registry")
System_Ext(s3, "S3-compatible Storage", "Garage local / cloud S3")
Rel(user, spa, "Uses", "HTTPS")
Rel(spa, platform, "API calls", "REST via gateway + JWT")
Rel(integrator, platform, "API calls", "REST via gateway + JWT")
Rel(platform, kafka, "Publishes/Consumes events", "outbox relay")
Rel(platform, pgx, "Owns data", "JPA/Flyway")
Rel(platform, s3, "Objects", "S3 API")
```

## 4. Distributed Target — Containers (C4 L2)

```mermaid
C4Container
title Future State — Containers (per bounded context)
Container(gw, "gateway", "Spring Cloud Gateway", "Routes, JWT via JWKS, rate limits; strangler default to monolith")
Container(idp, "identity-svc", "Spring Authorization Server", "OIDC, JWT w/ party-role claims")
Container(party, "party-svc", "Spring Boot 3.3 / Java 21", "Parties, roles, contacts (133 entities)")
Container(product, "product-svc", "Spring Boot 3.3", "Catalog, pricing (~192)")
Container(order, "order-svc", "Spring Boot 3.3", "Order lifecycle, returns, quotes (114)")
Container(bill, "billing-svc", "Spring Boot 3.3", "Invoices, payments, gateways (~94)")
Container(fin, "finance-svc", "Spring Boot 3.3", "GL, postings, periods (~90)")
Container(inv, "inventory-svc", "Spring Boot 3.3", "Facilities, stock, shipments (64)")
Container(tail, "7 more domain svcs", "Spring Boot 3.3", "manufacturing, content, workeffort, humanres, marketing")
Container(search, "search-svc", "OpenSearch client", "Index/query projections")
Container(notif, "notification-svc", "Spring Boot 3.3", "Email/push on events")
Container(audit, "audit-svc", "Spring Boot 3.3", "Wildcard event store, secret audit")
ContainerDb(pg1, "party DB", "Postgres", "owned entities")
ContainerDb(pg2, "order DB", "Postgres", "owned entities")
ContainerQueue(k, "Kafka", "Strimzi 3 brokers", "context.aggregate.action.v1")
ContainerDb(os, "OpenSearch", "Log/audit analytics", "ofbiz-logs")
ContainerDb(loki, "Loki", "Log aggregation", "dashboards")
Container(monolith, "OFBiz Monolith (strangled)", "legacy", "served until its phase cutover")
Rel(gw, idp, "Validates JWT", "JWKS")
Rel(gw, order, "/api/v1/orders/**", "REST")
Rel(gw, monolith, "/** fallback", "strangler route + CDC reverse-sync")
Rel(order, party, "Validate bill-to", "gRPC (mesh mTLS)")
Rel(order, product, "Price snapshot", "gRPC")
Rel(order, k, "order.order.placed", "outbox relay")
Rel(k, bill, "order.order.completed", "consumer")
Rel(k, fin, "billing.invoice.issued", "consumer")
Rel(k, audit, "wildcard", "consumer")
Rel(k, search, "product/content changes", "indexer")
Rel(party, pg1, "owns", "JPA")
Rel(order, pg2, "owns", "JPA")
Rel(audit, os, "retention", "index")
```

## 5. Component View — order-svc exemplar (C4 L3)

```mermaid
C4Component
title order-svc — Component Diagram (pattern for all 11 domain services)
Container(svc, "order-svc", "Spring Boot 3.3")
Component(domain, "order-domain", "Pure Java 21", "Order aggregate, status machine, use-case ports — zero Spring")
Component(app, "order-application", "Spring", "PlaceOrder use case, saga orchestration, outbox writes, TX boundaries")
Component(grpc, "adapters-grpc", "gRPC", "OrderService endpoints (order.v1 proto)")
Component(rest, "adapters-rest", "REST", "/api/v1/orders (OpenAPI via gateway)")
Component(kafkaa, "adapters-kafka", "Kafka", "Consumes price/party/stock events; relay publishes outbox")
Component(infra, "infrastructure", "JPA/Flyway", "Repos, Postgres config, product/party gRPC clients, OTel")
Rel(grpc, app, "invoke use case")
Rel(rest, app, "invoke use case")
Rel(kafkaa, app, "deliver events")
Rel(app, domain, "calls pure logic")
Rel(infra, app, "repo impls / clients")
```

## 6. Sequence — OIDC Login & API Call

```mermaid
sequenceDiagram
 autonumber
 actor U as User
 participant SPA as Angular SPA
 participant GW as gateway
 participant IDP as identity-svc
 participant SVC as domain-svc
 U->>SPA: open app
 SPA->>GW: GET /api/v1/... (no token)
 GW-->>SPA: 401
 SPA->>GW: authorize (PKCE)
 GW->>IDP: OIDC auth-code flow
 IDP-->>SPA: code + state
 SPA->>IDP: token exchange
 IDP-->>SPA: JWT (party-role claims)
 SPA->>GW: GET /api/v1/... + JWT
 GW->>IDP: JWKS validate
 GW->>SVC: gRPC/REST + traceparent
 SVC-->>GW: 200 + OTel span
 GW-->>SPA: 200
```

## 7. Sequence — Place Order Saga (outbox + events)

```mermaid
sequenceDiagram
 autonumber
 participant SPA as SPA
 participant GW as gateway
 participant ORD as order-svc
 participant PDB as order DB
 participant K as Kafka
 participant INV as inventory-svc
 participant PROD as product-svc
 participant AUD as audit-svc
 SPA->>GW: POST /api/v1/orders
 GW->>ORD: placeOrder (JWT validated)
 ORD->>PROD: gRPC price snapshot
 PROD-->>ORD: prices
 ORD->>PDB: TX insert Order + outbox(order.order.placed)
 ORD-->>GW: 201 (placed=pending)
 Note over ORD,K: outbox relay (2s poll) publishes
 ORD->>K: order.order.placed.v1
 K->>INV: consume then reserve stock
 INV->>K: inventory.stock.reserved.v1
 K->>ORD: consume then status=confirmed
 K->>AUD: wildcard consume
 Note over INV: on failure compensate + order.cancelled
```

## 8. Sequence — Outbox Publish (atomicity)

```mermaid
sequenceDiagram
 autonumber
 participant APP as application layer
 participant DB as service Postgres
 participant R as outbox relay
 participant K as Kafka
 participant C as consumer svc
 APP->>DB: BEGIN
 APP->>DB: UPDATE aggregate
 APP->>DB: INSERT outbox(event)
 APP->>DB: COMMIT (atomic)
 loop every 2s, FOR UPDATE SKIP LOCKED
   R->>DB: claim unpublished rows
   R->>K: publish Avro (key=aggregate_id)
   R->>DB: mark published_at
 end
 K->>C: deliver
 C->>C: insert processed_events(event_id) — skip if exists
 C->>C: handle idempotently
```

## 9. Sequence — Strangler CDC Bridge (write cutover)

```mermaid
sequenceDiagram
 autonumber
 participant W as gateway route flip
 participant SVC as migrating svc
 participant DEB as Debezium CDC
 participant K as Kafka
 participant MONO as OFBiz monolith DB
 participant CHK as consistency checker
 Note over MONO,DEB: pre-cutover: monolith authoritative
 MONO->>DEB: WAL changes
 DEB->>K: ofbiz.entity.action
 K->>SVC: build projection tables
 W->>SVC: /api/v1/ctx/** now routed to svc
 SVC->>K: domain events (authoritative)
 K->>MONO: reverse-sync adapter writes back (legacy consumers)
 loop continuous
   CHK->>SVC: row counts + checksums
   CHK->>MONO: compare
   CHK->>CHK: alert on delta non-zero (freeze-write protocol)
 end
```

## 10. Sequence — Content Upload to S3 (Garage local / cloud s3)

```mermaid
sequenceDiagram
 autonumber
 participant SPA as SPA
 participant GW as gateway
 participant CT as content-svc
 participant S3 as S3 API (Garage/cloud)
 participant K as Kafka
 SPA->>GW: POST /api/v1/content (multipart)
 GW->>CT: route + JWT
 CT->>S3: PutObject ofbiz-content/id
 S3-->>CT: etag
 CT->>CT: DB metadata + outbox(content.content.published)
 CT-->>GW: 201 (object URL)
 CT->>K: content.content.published.v1
 Note over CT,S3: cloud-native swap = endpoint+creds only (ADR-008)
```

## 11. Deployment View — Local Platform (as-built, Phase 0)

```mermaid
graph TB
    subgraph kind["kind cluster: ofbiz"]
        subgraph istio["istio-system"]
            IOD[istiod STRICT mTLS]
        end
        subgraph ns["ofbiz"]
            KFK[Strimzi Kafka 3 brokers]
            REG[Apicurio registry]
        end
        subgraph obs["observability"]
            JAE[Jaeger OTLP/zipkin]
            PROM[Prometheus]
            GRAF[Grafana 4 datasources]
            LOKI[Loki + promtail]
            OS[OpenSearch + fluent-bit]
        end
        subgraph st["storage"]
            GAR[Garage S3 + buckets]
        end
    end
    GRAF --> PROM & LOKI & OS & JAE
    PROM --> IOD
```

---

*Maintained alongside the architecture docs; topology changes update §3-4 in the same PR (CLAUDE.md process rules).*
