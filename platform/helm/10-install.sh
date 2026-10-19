#!/usr/bin/env bash
# P0-F4-S2: deploy demo-svc via shared service-chart into the kind cluster.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TAG="${1:-dev}"

kind load docker-image demo-svc:"$TAG" --name ofbiz || true
helm upgrade --install demo-svc "${SCRIPT_DIR}/service-chart" \
  --namespace ofbiz \
  --set image.repository=demo-svc \
  --set image.tag="$TAG" \
  --set autoscaling.enabled=true
kubectl -n ofbiz rollout status deploy/demo-svc --timeout=180s
