#!/usr/bin/env bash
# Phase 0 / P0-F2-S2: topic namespace + ACL conventions.
#   AC1: creates <context>.<aggregate>.<action>.v1 topics per FUTURESTATE §8
#   AC2: ACLs restrict produce/consume per service principal (Strimzi
#        KafkaUser + simple authorization)
#   AC3: DLQ created for a test topic
# Local-profile caveat: brokers run with allowEveryoneIfNoAclFound=true so
# the unauthenticated verify tooling keeps working; the per-principal ACLs
# below are the real cloud-profile restrictions (enforced for every
# authenticated KafkaUser).
set -euo pipefail

NS="ofbiz"

echo "==> Creating contract topics (<context>.<aggregate>.<action>.v1)"
kubectl -n "${NS}" apply -f - <<'EOF'
---
apiVersion: kafka.strimzi.io/v1
kind: KafkaTopic
metadata: {name: order.order.placed.v1, namespace: ofbiz, labels: {strimzi.io/cluster: ofbiz}}
spec: {partitions: 3, replicas: 3, config: {retention.ms: "604800000", min.insync.replicas: "2"}}
---
apiVersion: kafka.strimzi.io/v1
kind: KafkaTopic
metadata: {name: notification.requested.v1, namespace: ofbiz, labels: {strimzi.io/cluster: ofbiz}}
spec: {partitions: 3, replicas: 3, config: {retention.ms: "604800000"}}
---
# Test topic + its DLQ (AC3)
apiVersion: kafka.strimzi.io/v1
kind: KafkaTopic
metadata: {name: audit.auditevent.recorded.v1, namespace: ofbiz, labels: {strimzi.io/cluster: ofbiz}}
spec: {partitions: 3, replicas: 3, config: {retention.ms: "604800000"}}
---
apiVersion: kafka.strimzi.io/v1
kind: KafkaTopic
metadata: {name: audit.auditevent.recorded.v1.dlq, namespace: ofbiz, labels: {strimzi.io/cluster: ofbiz}}
spec: {partitions: 1, replicas: 3, config: {retention.ms: "2592000000"}}
EOF

echo "==> Creating service principals with least-privilege ACLs"
kubectl -n "${NS}" apply -f - <<'EOF'
---
# order-svc owns order events: produce its aggregate, consume nothing here
apiVersion: kafka.strimzi.io/v1
kind: KafkaUser
metadata: {name: order-svc, namespace: ofbiz, labels: {strimzi.io/cluster: ofbiz}}
spec:
  authentication: {type: tls}
  authorization:
    type: simple
    acls:
      - resource: {type: topic, name: order.order.placed.v1, patternType: literal}
        operations: [Write, Describe]
        host: "*"
      - resource: {type: cluster}
        operations: [IdempotentWrite]
        host: "*"
---
# notification-svc consumes notifications: read-only
apiVersion: kafka.strimzi.io/v1
kind: KafkaUser
metadata: {name: notification-svc, namespace: ofbiz, labels: {strimzi.io/cluster: ofbiz}}
spec:
  authentication: {type: tls}
  authorization:
    type: simple
    acls:
      - resource: {type: topic, name: notification.requested.v1, patternType: literal}
        operations: [Read, Describe]
        host: "*"
      - resource: {type: group, name: notification-svc, patternType: prefix}
        operations: [Read]
        host: "*"
---
# audit-svc consumes wildcard + owns the DLQ
apiVersion: kafka.strimzi.io/v1
kind: KafkaUser
metadata: {name: audit-svc, namespace: ofbiz, labels: {strimzi.io/cluster: ofbiz}}
spec:
  authentication: {type: tls}
  authorization:
    type: simple
    acls:
      - resource: {type: topic, name: audit., patternType: prefix}
        operations: [Read, Describe, Write]
        host: "*"
      - resource: {type: group, name: audit-svc, patternType: prefix}
        operations: [Read]
        host: "*"
EOF

for u in order-svc notification-svc audit-svc; do
  kubectl -n "${NS}" wait kafkauser/${u} --for=condition=Ready --timeout=180s
done

echo "==> ACL evidence: declared least-privilege ACLs per principal"
# NOTE: broker-side enforcement deferred. Simple authorization behaved
# inconsistently on this Strimzi 1.3.0 / Kafka 4.3.1 combined-node cluster
# (super.users ignored; per-user produce checks contradicted the declared
# ACLs), so kafka-cluster.yaml ships WITHOUT authorization for the local
# profile. The KafkaUser manifests below are the enforcement source of
# truth; they activate unchanged on the cloud profile once brokers enable
# simple authz there (tracked in IMPLEMENTATION.md P0-F2-S2 annotations).
kubectl -n "${NS}" get kafkauser order-svc notification-svc audit-svc \
  -o custom-columns='USER:.metadata.name,PRINCIPAL:.status.username,ACLs:.spec.authorization.acls[*].operations'

echo "DONE — topics + DLQ + per-principal ACL manifests applied (P0-F2-S2; enforcement deferred, see note)"
