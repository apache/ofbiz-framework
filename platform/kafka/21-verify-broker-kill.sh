#!/usr/bin/env bash
# Phase 0 / P0-F2-S1 AC: one-broker kill causes no data loss.
# Produces 100 messages, deletes a broker pod, waits for the cluster to
# rebalance + broker to return, then consumes everything back.
set -euo pipefail

NAMESPACE="ofbiz"
TOPIC="p0-verify-broker-kill"
MSGS=100

echo "==> Creating topic ${TOPIC} (rf 3, min.isr 2)"
kubectl -n "${NAMESPACE}" apply -f - <<EOF
apiVersion: kafka.strimzi.io/v1
kind: KafkaTopic
metadata: {name: ${TOPIC}, namespace: ${NAMESPACE}, labels: {strimzi.io/cluster: ofbiz}}
spec: {partitions: 3, replicas: 3, config: {min.insync.replicas: "2"}}
EOF
kubectl -n "${NAMESPACE}" wait kafkatopic/${TOPIC} --for=condition=Ready --timeout=120s

PODS=($(kubectl -n "${NAMESPACE}" get pods -l strimzi.io/pool-name=ofbiz-nodes -o name | cut -d/ -f2 | sort))
VICTIM="${PODS[2]}"
SURVIVOR="${PODS[0]}"
echo "==> Producing ${MSGS} messages (acks=all)"
kubectl -n "${NAMESPACE}" exec -i "${SURVIVOR}" -- bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 --request-required-acks all --topic "${TOPIC}" \
  <<< "$(for i in $(seq 1 ${MSGS}); do echo "msg-${i}"; done)" >/dev/null

echo "==> Killing broker pod ${VICTIM}"
kubectl -n "${NAMESPACE}" delete pod "${VICTIM}" --wait=false >/dev/null
kubectl -n "${NAMESPACE}" wait --for=delete "pod/${VICTIM}" --timeout=120s || true

echo "==> Waiting for kafka/ofbiz to return to Ready"
sleep 10
kubectl -n "${NAMESPACE}" wait kafka/ofbiz --for=condition=Ready --timeout=600s

echo "==> Consuming all messages (expect ${MSGS} unique)"
OUT="$(kubectl -n "${NAMESPACE}" exec "${SURVIVOR}" -- bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic "${TOPIC}" --from-beginning --timeout-ms 30000 2>/dev/null || true)"
UNIQ="$(echo "${OUT}" | grep -c '^msg-')"
DUP="$(echo "${OUT}" | sort | uniq -d | wc -l | tr -d ' ')"

if [ "${UNIQ}" -eq "${MSGS}" ] && [ "${DUP}" -eq 0 ]; then
  echo "PASS: ${UNIQ}/${MSGS} messages survived broker kill, 0 duplicates"
else
  echo "FAIL: got ${UNIQ} unique, ${DUP} duplicated"; exit 1
fi
echo "ALL CHECKS PASSED — evidence above satisfies P0-F2-S1 broker-kill AC"
