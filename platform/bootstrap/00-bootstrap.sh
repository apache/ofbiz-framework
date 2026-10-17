#!/usr/bin/env bash
# Phase 0 / P0-F1-S1: create the local kind cluster, install Istio with
# strict mTLS, and create the service namespace with injection enabled.
#
# Usage: sh platform/bootstrap/00-bootstrap.sh
# Requires: kind, kubectl, istioctl (see platform/README.md).
set -euo pipefail

CLUSTER_NAME="ofbiz"
NAMESPACE="ofbiz"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "==> Creating kind cluster '${CLUSTER_NAME}' (idempotent)"
if kind get clusters 2>/dev/null | grep -q "^${CLUSTER_NAME}$"; then
  echo "    cluster already exists, reusing"
else
  kind create cluster --config "${SCRIPT_DIR}/../kind/cluster.yaml"
fi
kubectl config use-context "kind-${CLUSTER_NAME}"

echo "==> Installing Istio (minimal profile)"
istioctl install --set profile=minimal --skip-confirmation

echo "==> Enforcing STRICT mesh-wide mTLS"
kubectl apply -f - <<'EOF'
apiVersion: security.istio.io/v1
kind: PeerAuthentication
metadata:
  name: mesh-strict-mtls
  namespace: istio-system
spec:
  mtls:
    mode: STRICT
EOF

echo "==> Creating namespace '${NAMESPACE}' with sidecar injection"
kubectl create namespace "${NAMESPACE}" --dry-run=client -o yaml | kubectl apply -f -
kubectl label namespace "${NAMESPACE}" istio-injection=enabled --overwrite

echo "==> Status"
kubectl get pods -n istio-system
kubectl get namespace "${NAMESPACE}" --show-labels
echo "Bootstrap complete. Verify mTLS with platform/bootstrap/10-verify-mtls.sh"
