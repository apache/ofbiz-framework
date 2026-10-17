# platform/ — Shared Infrastructure (Phase 0)

Everything later phases deploy onto. See [IMPLEMENTATION.md](../IMPLEMENTATION.md) Phase 0 and [FUTURESTATE-LOWLEVEL.md](../FUTURESTATE-LOWLEVEL.md) §6.

## Layout

```
platform/
  kind/cluster.yaml           # local cluster definition (3 nodes, ingress ports 80/443)
  bootstrap/
    00-bootstrap.sh           # create cluster + Istio STRICT mTLS + 'ofbiz' namespace
    10-verify-mtls.sh         # P0-F1-S1 AC evidence (exit 0 = pass)
  helm/                       # umbrella + service charts (added in later stories)
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
