#!/usr/bin/env bash
# Local mirror of .github/workflows/ci.yml — same gates, no GitHub needed.
# Exits non-zero on any gate failure, exactly like the CI run would.
#   build → trivy (HIGH/CRITICAL gate) → Avro contracts (BACKWARD 409 gate)
# Optional: PUSH=1 also pushes to GHCR (requires docker login ghcr.io).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "${ROOT}"

echo "==> [1/4] Build, unit + contract tests (ArchUnit enforced)"
./gradlew build

echo "==> [2/4] Service image"
docker build -t demo-svc:local platform/demo-svc

echo "==> [3/4] Trivy gate (HIGH/CRITICAL, unfixed only)"
trivy image --severity HIGH,CRITICAL --ignore-unfixed --exit-code 1 demo-svc:local

echo "==> [4/4] Avro contracts gate (Apicurio, BACKWARD)"
docker rm -f local-ci-apicurio >/dev/null 2>&1 || true
docker run -d --name local-ci-apicurio -p 18080:8080 \
  quay.io/apicurio/apicurio-registry-mem:2.6.x-release >/dev/null
trap 'docker rm -f local-ci-apicurio >/dev/null 2>&1 || true' EXIT
for i in $(seq 1 30); do
  curl -sf -o /dev/null http://localhost:18080/apis/registry/v2/system/info && break
  sleep 2
done
REG="http://localhost:18080/apis/registry/v2/groups/order/artifacts"
curl -sf -X POST "${REG}?ifExists=RETURN" \
  -H "X-Registry-ArtifactId: order-placed" -H "X-Registry-ArtifactType: AVRO" \
  -H "Content-Type: application/json" \
  --data-binary @contracts/order/v1/OrderPlaced.avsc >/dev/null
curl -sf -X POST "${REG}/order-placed/rules" \
  -H "Content-Type: application/json" \
  -d '{"type":"COMPATIBILITY","config":"BACKWARD"}' >/dev/null
# incompatible variant (adds required field, no default) must get 409
sed 's/"name": "lineCount", "type": "int" }/"name": "lineCount", "type": "int" },\n    { "name": "riskScore", "type": "int" }/' \
  contracts/order/v1/OrderPlaced.avsc > /tmp/local-ci-incompatible.avsc
CODE="$(curl -s -o /tmp/local-ci-resp -w '%{http_code}' -X POST "${REG}/order-placed/versions" \
  -H "Content-Type: application/json" --data-binary @/tmp/local-ci-incompatible.avsc)"
echo "    incompatible register → HTTP ${CODE}"
[ "${CODE}" = "409" ] || { echo "FAIL: expected 409"; cat /tmp/local-ci-resp; exit 1; }

if [ "${PUSH:-0}" = "1" ]; then
  echo "==> Pushing to GHCR"
  docker tag demo-svc:local "ghcr.io/kensemerkant/demo-svc:local"
  docker push ghcr.io/kensemerkant/demo-svc:local
fi

echo "LOCAL CI PASSED — same gates as .github/workflows/ci.yml"
