#!/usr/bin/env bash
# Phase 0 / P0-F2-S1: install Strimzi operator + 3-broker Kafka (KRaft)
# and Apicurio schema registry into the local cluster.
# Idempotent. Override versions via env: STRIMZI_VERSION, APICURIO_IMAGE.
set -euo pipefail

NAMESPACE="ofbiz"
STRIMZI_VERSION="${STRIMZI_VERSION:-1.3.0}"
APICURIO_IMAGE="${APICURIO_IMAGE:-quay.io/apicurio/apicurio-registry-mem:2.6.x-release}"

echo "==> Installing Strimzi CRDs (pinned ${STRIMZI_VERSION})"
kubectl apply --server-side -f "https://github.com/strimzi/strimzi-kafka-operator/releases/download/${STRIMZI_VERSION}/strimzi-crds-${STRIMZI_VERSION}.yaml" >/dev/null \
  || { echo "FATAL: Strimzi ${STRIMZI_VERSION} CRDs not reachable"; exit 1; }

echo "==> Installing Strimzi operator (namespace-scoped, watches ${NAMESPACE})"
curl -sL "https://strimzi.io/install/latest?namespace=${NAMESPACE}" | kubectl apply -n "${NAMESPACE}" -f - >/dev/null \
  || { echo "FATAL: Strimzi install bundle not reachable"; exit 1; }
kubectl -n "${NAMESPACE}" wait deploy/strimzi-cluster-operator --for=condition=Available --timeout=300s

echo "==> Creating Kafka cluster (KRaft, 3 brokers, small local resources)"
kubectl -n "${NAMESPACE}" apply -f "$(dirname "$0")/kafka-cluster.yaml"
kubectl -n "${NAMESPACE}" wait kafka/ofbiz --for=condition=Ready --timeout=600s

echo "==> Deploying Apicurio schema registry (in-memory, local profile)"
kubectl -n "${NAMESPACE}" apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata: {name: schema-registry, namespace: ${NAMESPACE}}
spec:
  replicas: 1
  selector: {matchLabels: {app: schema-registry}}
  template:
    metadata:
      labels: {app: schema-registry}
      annotations: {sidecar.istio.io/inject: "true"}
    spec:
      containers:
        - name: registry
          image: "${APICURIO_IMAGE}"
          ports: [{containerPort: 8080}]
          env:
            - {name: REGISTRY_UI_ENABLED, value: "true"}
          readinessProbe: {httpGet: {path: /health/ready, port: 8080}, initialDelaySeconds: 10}
---
apiVersion: v1
kind: Service
metadata: {name: schema-registry, namespace: ${NAMESPACE}}
spec: {selector: {app: schema-registry}, ports: [{port: 8080}]}
EOF
kubectl -n "${NAMESPACE}" rollout status deploy/schema-registry --timeout=300s

echo "Kafka + schema registry ready. Verify with platform/kafka/20-verify.sh"
