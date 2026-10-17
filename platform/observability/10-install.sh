#!/usr/bin/env bash
# Phase 0 / P0-F3 (expanded): observability from inception —
# Jaeger (tracing), Prometheus (metrics), Grafana+Loki (dashboards/logs),
# OpenSearch (log analytics/audit retention).
# Idempotent. Requires the bootstrapped cluster (00-bootstrap.sh).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
NS="observability"

echo "==> Pointing Istio mesh tracing at Jaeger OTLP"
istioctl install --set profile=minimal --skip-confirmation \
  --set meshConfig.enableTracing=true \
  --set "meshConfig.extensionProviders[0].name=otel-tracing" \
  --set "meshConfig.extensionProviders[0].otlp.service=jaeger-collector.observability.svc.cluster.local" \
  --set "meshConfig.extensionProviders[0].otlp.port=4317" \
  --set "meshConfig.defaultProviders.tracing[0]=otel-tracing" >/dev/null

echo "==> Applying observability stack"
kubectl apply -f "${SCRIPT_DIR}/observability-stack.yaml" >/dev/null

echo "==> Waiting for rollouts"
for d in jaeger prometheus loki grafana; do
  kubectl -n "${NS}" rollout status "deploy/${d}" --timeout=300s
done
kubectl -n "${NS}" rollout status statefulset/opensearch --timeout=600s
kubectl -n "${NS}" rollout status daemonset/promtail --timeout=300s
kubectl -n "${NS}" rollout status daemonset/filebeat --timeout=300s

echo "Observability stack ready. Verify with platform/observability/20-verify.sh"
