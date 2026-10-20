# platform/ — Shared Infrastructure (Phase 0)

Everything later phases deploy onto. See [IMPLEMENTATION.md](../IMPLEMENTATION.md) Phase 0 and [FUTURESTATE-LOWLEVEL.md](../FUTURESTATE-LOWLEVEL.md) §6.

## Layout

```
platform/
  kind/cluster.yaml           # local cluster definition (3 nodes, ingress ports 80/443)
  bootstrap/
    00-bootstrap.sh           # create cluster + Istio STRICT mTLS + 'ofbiz' namespace
    10-verify-mtls.sh         # P0-F1-S1 AC evidence (exit 0 = pass)
  observability/               # Jaeger, Prometheus, Grafana/Loki, OpenSearch stack
  storage/                    # Garage local S3 (MinIO images purged 2025, see ADR-008)
  kafka/                      # Strimzi Kafka + Apicurio schema registry
  certmanager/                # cert-manager + istio ingress + wildcard TLS
  vault/                      # Vault dev + Secrets Store CSI (P0-F1-S3)
  helm/                       # umbrella + service charts; topics/ = Kafka topic+ACL conventions
  terraform/                  # cloud equivalent of kind/ (added when cluster target chosen)
```

## Local cluster (kind)

Prereqs (macOS): `brew install kind kubectl istioctl helm`

```bash
sh platform/bootstrap/00-bootstrap.sh     # idempotent
sh platform/bootstrap/10-verify-mtls.sh   # proves STRICT mTLS; prints gate evidence
```

## Verification status

| Story | Evidence |
|---|---|
| P0-F1-S1 (cluster + STRICT mTLS) | run `10-verify-mtls.sh`, expect `ALL CHECKS PASSED` |
| P0-F1-S2 (cert-manager + ingress TLS) | run `certmanager/20-verify.sh`, expect `ALL CHECKS PASSED` |
| P0-F1-S3 (Vault CSI) | run `vault/20-verify.sh`, expect `ALL CHECKS PASSED` |
| P0-F2-S1 (Kafka + registry) | run `kafka/20-verify.sh` (roundtrip) and `kafka/21-verify-broker-kill.sh` (no data loss) |
| P0-F2-S2 (topics + ACL conventions) | run `helm/topics/10-create.sh`; ACL enforcement deferred — see script note |
| P0-F2-S3 (Avro contracts) | CI `contracts` job in `.github/workflows/ci.yml` |
| P0-F3 (observability, expanded) | run `observability/20-verify.sh`, expect `ALL CHECKS PASSED` |
| P0 storage (local S3 — Garage) | run `storage/20-verify.sh`, expect `ALL CHECKS PASSED` |
