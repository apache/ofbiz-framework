#!/usr/bin/env bash
# Phase 0 / P0-F1-S1 acceptance criteria: prove STRICT mTLS.
#   AC1: sidecar-injected namespace exists
#   AC2: plaintext (sidecar-less) call FAILS; mesh (sidecar) call SUCCEEDS
# Exit 0 = all pass. Evidence printed for the gate record.
set -euo pipefail

NAMESPACE="ofbiz"
NO_MESH_NS="ofbiz-nomesh"

cleanup() {
  kubectl -n "${NAMESPACE}" delete deploy httpbin verify-sleep --ignore-not-found >/dev/null 2>&1 || true
  kubectl delete namespace "${NO_MESH_NS}" --ignore-not-found >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "==> AC1: namespace ${NAMESPACE} has sidecar injection"
kubectl get namespace "${NAMESPACE}" -o jsonpath='{.metadata.labels.istio-injection}' | grep -q enabled \
  && echo "PASS: istio-injection=enabled" || { echo "FAIL: injection label missing"; exit 1; }

echo "==> Deploying test workloads (httpbin + sleep in mesh)"
kubectl -n "${NAMESPACE}" apply -f - <<'EOF'
apiVersion: apps/v1
kind: Deployment
metadata: {name: httpbin, namespace: ofbiz}
spec:
  replicas: 1
  selector: {matchLabels: {app: httpbin}}
  template:
    metadata: {labels: {app: httpbin}}
    spec: {containers: [{name: httpbin, image: docker.io/kennethreitz/httpbin, ports: [{containerPort: 8000}]}]}
---
apiVersion: v1
kind: Service
metadata: {name: httpbin, namespace: ofbiz}
spec: {selector: {app: httpbin}, ports: [{port: 8000, targetPort: 80}]}
---
apiVersion: apps/v1
kind: Deployment
metadata: {name: verify-sleep, namespace: ofbiz}
spec:
  replicas: 1
  selector: {matchLabels: {app: verify-sleep}}
  template:
    metadata: {labels: {app: verify-sleep}}
    spec: {containers: [{name: sleep, image: curlimages/curl, command: ["sleep", "3600"]}]}
EOF

echo "==> Waiting for readiness"
kubectl -n "${NAMESPACE}" rollout status deploy/httpbin --timeout=180s
kubectl -n "${NAMESPACE}" rollout status deploy/verify-sleep --timeout=180s

echo "==> AC2a: mesh call (sidecar to sidecar) must SUCCEED"
MESH_OK=0
for attempt in 1 2 3 4 5 6; do
  CODE="$(kubectl -n "${NAMESPACE}" exec deploy/verify-sleep -c sleep -- \
       curl -sS -o /dev/null -w '%{http_code}' --max-time 10 http://httpbin:8000/get 2>&1 || true)"
  echo "    attempt ${attempt}: ${CODE}"
  if [ "${CODE}" = "200" ]; then MESH_OK=1; break; fi
  sleep 5
done
if [ "${MESH_OK}" = "1" ]; then
  echo "PASS: mTLS call returned 200"
else
  echo "FAIL: mesh call never returned 200 (last: ${CODE})"; exit 1
fi

echo "==> AC2b: plaintext call (no sidecar) must FAIL under STRICT"
kubectl create namespace "${NO_MESH_NS}" --dry-run=client -o yaml | kubectl apply -f - >/dev/null
kubectl -n "${NO_MESH_NS}" run plain-curl --image=curlimages/curl --command -- sleep 60 >/dev/null
kubectl -n "${NO_MESH_NS}" wait --for=condition=Ready pod/plain-curl --timeout=120s
if kubectl -n "${NO_MESH_NS}" exec plain-curl -- \
     curl -sS -o /dev/null --max-time 10 http://httpbin.ofbiz.svc.cluster.local:8000/get 2>/dev/null; then
  echo "FAIL: plaintext call succeeded — STRICT mTLS NOT enforced"; exit 1
else
  echo "PASS: plaintext call rejected"
fi

echo "ALL CHECKS PASSED — evidence above satisfies P0-F1-S1 AC"
