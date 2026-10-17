#!/usr/bin/env bash
# Phase 0 / P0-F2-S1 acceptance criteria: Kafka produce/consume roundtrip
# through a Strimzi Kafka pod. Exit 0 = pass.
#   AC: topic create + produce + consume from ops pod
set -euo pipefail

NAMESPACE="ofbiz"
TOPIC="p0-verify-roundtrip"
MSG="phase0-$(date +%s)"

echo "==> Creating topic ${TOPIC} (replication 3)"
kubectl -n "${NAMESPACE}" apply -f - <<EOF
apiVersion: kafka.strimzi.io/v1
kind: KafkaTopic
metadata: {name: ${TOPIC}, namespace: ${NAMESPACE}, labels: {strimzi.io/cluster: ofbiz}}
spec: {partitions: 3, replicas: 3}
EOF
kubectl -n "${NAMESPACE}" wait kafkatopic/${TOPIC} --for=condition=Ready --timeout=120s

POD="$(kubectl -n "${NAMESPACE}" get pods -l strimzi.io/pool-name=ofbiz-nodes -o name | head -1 | cut -d/ -f2)"
echo "==> Using broker pod ${POD}"

echo "==> Producing message: ${MSG}"
kubectl -n "${NAMESPACE}" exec -i "${POD}" -- bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 --topic "${TOPIC}" <<< "${MSG}" >/dev/null

echo "==> Consuming (expect the same message)"
OUT="$(kubectl -n "${NAMESPACE}" exec "${POD}" -- bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic "${TOPIC}" --from-beginning --max-messages 1 --timeout-ms 15000 2>/dev/null | head -1)"

if [ "${OUT}" = "${MSG}" ]; then
  echo "PASS: roundtrip '${OUT}'"
else
  echo "FAIL: expected '${MSG}', got '${OUT}'"; exit 1
fi

echo "ALL CHECKS PASSED — evidence above satisfies P0-F2-S1 roundtrip AC"
