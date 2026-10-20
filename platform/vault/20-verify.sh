#!/usr/bin/env bash
# Phase 0 / P0-F1-S3 AC:
#   AC1: test secret mounted into pod via Vault CSI
#   AC2: pod sees the expected synthetic values
# Rotation doc: platform/vault/README.md
set -euo pipefail

NS="ofbiz"

echo "==> Waiting for the CSI mount (provider writes files after pod start)"
FOUND=""
for attempt in $(seq 1 12); do
  U="$(kubectl -n "${NS}" exec vault-csi-test -- cat /mnt/secrets/username 2>/dev/null || true)"
  P="$(kubectl -n "${NS}" exec vault-csi-test -- cat /mnt/secrets/password 2>/dev/null || true)"
  if [ "${U}" = "demo-user" ] && [ "${P}" = "demo-pass" ]; then FOUND=1; break; fi
  sleep 10
done

if [ "${FOUND}" = "1" ]; then
  echo "PASS: Vault CSI mounted secret/ofbiz/demo (username=${U})"
else
  echo "FAIL: mounted values not found (username='${U}')"; exit 1
fi

echo "ALL CHECKS PASSED — evidence above satisfies P0-F1-S3 AC"
