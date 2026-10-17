# Gate CG-0 — Phase 0: Infrastructure Build

**Type:** phase completion gate
**Objective:** everything later phases deploy onto exists and is provable: cluster, mesh, Kafka + schema registry, observability stack, CI/CD, and the repository scaffolding/conventions from FUTURESTATE-LOWLEVEL §1.
**Depends on:** all phase 0 stories

## Checklist (all must pass, evidence linked)
- [ ] `kubectl get pods -A` green on target cluster; mesh-injected namespace `ofbiz` exists
- [ ] Kafka 3.7 (Strimzi) + Schema Registry reachable from a test pod; topic create/consume roundtrip proven
- [ ] OTel collector receiving spans/metrics from a demo deployment; Jaeger trace and Grafana dashboard visible
- [ ] CI pipeline builds a hello-world Spring Boot 3.3 service (convention plugin) and pushes an image to registry; CD deploys it via the shared Helm chart with mTLS enforced
- [ ] Repo scaffold: `buildSrc` convention plugins compile; ArchUnit layering test enforces domain-purity on a sample module
