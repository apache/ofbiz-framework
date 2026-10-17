# Migration Issues Index

Auto-generated from IMPLEMENTATION.md. One file per story; `P<n>-GATE.md` = phase completion gate.

| File | ID | Title |
|---|---|---|
| [P0-GATE.md](issues/P0-GATE.md) | CG-0| Infrastructure Build |
| [P0-F1-S1.md](issues/P0-F1-S1.md) | P0-F1-S1| Provision cluster, `ofbiz` namespace, Istio with `PeerAuth: STRICT`. |
| [P0-F1-S2.md](issues/P0-F1-S2.md) | P0-F1-S2| cert-manager + wildcard DNS + gateway LB. |
| [P0-F1-S3.md](issues/P0-F1-S3.md) | P0-F1-S3| Vault CSI + K8s secrets baseline. |
| [P0-F2-S1.md](issues/P0-F2-S1.md) | P0-F2-S1| Strimzi Kafka 3.7 cluster (3 brokers) + Schema Registry. |
| [P0-F2-S2.md](issues/P0-F2-S2.md) | P0-F2-S2| Topic namespace + ACL conventions script (`platform/helm/topics`). |
| [P0-F2-S3.md](issues/P0-F2-S3.md) | P0-F2-S3| Avro contract pipeline. |
| [P0-F3-S1.md](issues/P0-F3-S1.md) | P0-F3-S1| OTel collector + Jaeger + Prometheus + Grafana via umbrella chart. |
| [P0-F3-S2.md](issues/P0-F3-S2.md) | P0-F3-S2| Log pipeline (Loki optional). |
| [P0-F4-S1.md](issues/P0-F4-S1.md) | P0-F4-S1| `buildSrc` convention plugins (`java-library`, `spring-boot-service`, `grpc`, `contracts-test`). |
| [P0-F4-S2.md](issues/P0-F4-S2.md) | P0-F4-S2| Shared Helm `service-chart`. |
| [P0-F4-S3.md](issues/P0-F4-S3.md) | P0-F4-S3| Pipeline: build → test → contract-publish → image → staging deploy. |
| [P0-F4-S4.md](issues/P0-F4-S4.md) | P0-F4-S4| ArchUnit layering rule in conventions. |
| [P1-GATE.md](issues/P1-GATE.md) | CG-1| Platform (Common) Services |
| [P1-F1-S1.md](issues/P1-F1-S1.md) | P1-F1-S1| Spring Authorization Server: auth-code+PKCE, client-credentials, JWKS endpoint. |
| [P1-F1-S2.md](issues/P1-F1-S2.md) | P1-F1-S2| Credential store + Flyway schema + user CRUD admin API. |
| [P1-F1-S3.md](issues/P1-F1-S3.md) | P1-F1-S3| Party-link projection consumer (stub for Phase 2). |
| [P1-F2-S1.md](issues/P1-F2-S1.md) | P1-F2-S1| Spring Cloud Gateway + JWKS JWT filter + route table. |
| [P1-F2-S2.md](issues/P1-F2-S2.md) | P1-F2-S2| Rate limit (Redis). |
| [P1-F2-S3.md](issues/P1-F2-S3.md) | P1-F2-S3| Strangler default route → monolith (staging monolith via docker image + JWT→header bridge). |
| [P1-F2-S4.md](issues/P1-F2-S4.md) | P1-F2-S4| CORS + security headers. |
| [P1-F3-S1.md](issues/P1-F3-S1.md) | P1-F3-S1| Wildcard consumer + partitioned Postgres store. |
| [P1-F3-S2.md](issues/P1-F3-S2.md) | P1-F3-S2| Secret-audit event schema + storage. |
| [P1-F4-S1.md](issues/P1-F4-S1.md) | P1-F4-S1| `notification.requested.v1` consumer + SMTP adapter + templates. |
| [P1-F5-S1.md](issues/P1-F5-S1.md) | P1-F5-S1| OpenSearch + consumer framework + query API. |
| [P2-GATE.md](issues/P2-GATE.md) | CG-2| party-svc + CDC Bridge |
| [P2-F1-S1.md](issues/P2-F1-S1.md) | P2-F1-S1| Clean-arch scaffold + Party aggregate (person/group, roles, relationships). |
| [P2-F1-S2.md](issues/P2-F1-S2.md) | P2-F1-S2| ContactMech + purposes. |
| [P2-F1-S3.md](issues/P2-F1-S3.md) | P2-F1-S3| FindPartiesByRole query + pagination. |
| [P2-F2-S1.md](issues/P2-F2-S1.md) | P2-F2-S1| Debezium connector config + `ofbiz.<entity>.<action>` topics + Avro schemas. |
| [P2-F2-S2.md](issues/P2-F2-S2.md) | P2-F2-S2| Projection builder library (platform module). |
| [P2-F2-S3.md](issues/P2-F2-S3.md) | P2-F2-S3| Reverse-sync adapter. |
| [P2-F2-S4.md](issues/P2-F2-S4.md) | P2-F2-S4| Consistency checker job. |
| [P2-F3-S1.md](issues/P2-F3-S1.md) | P2-F3-S1| JWT claims from party projection (roles→permissions). |
| [P3-GATE.md](issues/P3-GATE.md) | CG-3| product-svc + inventory-svc |
| [P3-F1-S1.md](issues/P3-F1-S1.md) | P3-F1-S1| Product aggregate + categories + associations (≈192 entities; mapping table checked in). |
| [P3-F1-S2.md](issues/P3-F1-S2.md) | P3-F1-S2| Pricing (price rules, currencies). |
| [P3-F1-S3.md](issues/P3-F1-S3.md) | P3-F1-S3| ProductStore/config exposure. |
| [P3-F2-S1.md](issues/P3-F2-S1.md) | P3-F2-S1| Facility + InventoryItem + stock ops (facility slice ≈17 + shipment 47 entities). |
| [P3-F2-S2.md](issues/P3-F2-S2.md) | P3-F2-S2| Reservation saga (reserve→confirm/cancel). |
| [P3-F2-S3.md](issues/P3-F2-S3.md) | P3-F2-S3| Shipment creation/packing/status. |
| [P3-F3-S1.md](issues/P3-F3-S1.md) | P3-F3-S1| product indexer consumer. |
| [P4-GATE.md](issues/P4-GATE.md) | CG-4| order-svc |
| [P4-F1-S1.md](issues/P4-F1-S1.md) | P4-F1-S1| Order aggregate + status machine (enum-guarded transitions). |
| [P4-F1-S2.md](issues/P4-F1-S2.md) | P4-F1-S2| Cart → order placement (price snapshot from product API/events). |
| [P4-F1-S3.md](issues/P4-F1-S3.md) | P4-F1-S3| Returns + adjustments. |
| [P4-F1-S4.md](issues/P4-F1-S4.md) | P4-F1-S4| Quotes. |
| [P4-F2-S1.md](issues/P4-F2-S1.md) | P4-F2-S1| placeOrder saga orchestrator (reserve stock, validate party, emit placed). |
| [P4-F2-S2.md](issues/P4-F2-S2.md) | P4-F2-S2| Marketing attribution consumer. |
| [P5-GATE.md](issues/P5-GATE.md) | CG-5| billing-svc + finance-svc |
| [P5-F1-S1.md](issues/P5-F1-S1.md) | P5-F1-S1| Invoice aggregate + issue/post/settle. |
| [P5-F1-S2.md](issues/P5-F1-S2.md) | P5-F1-S2| Payment aggregate + gateway adapter framework. |
| [P5-F1-S3.md](issues/P5-F1-S3.md) | P5-F1-S3| BillingAccount + terms. |
| [P5-F2-S1.md](issues/P5-F2-S1.md) | P5-F2-S1| GL: accounts, AcctgTrans postings, periods. |
| [P5-F2-S2.md](issues/P5-F2-S2.md) | P5-F2-S2| Auto-posting rules consumer (`billing.invoice.issued/settled` → AcctgTrans). |
| [P5-F2-S3.md](issues/P5-F2-S3.md) | P5-F2-S3| Trial-balance report API. |
| [P5-F3-S1.md](issues/P5-F3-S1.md) | P5-F3-S1| Accounting migration mapping doc + checker for both new DBs. |
| [P6-SHARED-S1.md](issues/P6-SHARED-S1.md) | P6-SHARED-S1| domain aggregate per entitymodel slice. |
| [P6-SHARED-S2.md](issues/P6-SHARED-S2.md) | P6-SHARED-S2| CDC + reverse-sync + consistency checker instantiation. |
| [P6-SHARED-S3.md](issues/P6-SHARED-S3.md) | P6-SHARED-S3| SPA screens for the domain. |
| [P7-S1.md](issues/P7-S1.md) | P7-S1| Route cleanup + bridge removal. |
| [P7-S2.md](issues/P7-S2.md) | P7-S2| Data archival. |
| [P7-S3.md](issues/P7-S3.md) | P7-S3| Ops handover. |
