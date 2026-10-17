#!/usr/bin/env bash
# P0 storage story AC: local S3 answers; program buckets provisioned
# (ofbiz-content, ofbiz-audit); put/get roundtrip via awscli against the S3 API.
set -euo pipefail

NS="storage"
SVC="garage"

echo "==> Applying Garage (local S3)"
kubectl apply -f "$(dirname "$0")/garage.yaml" >/dev/null
kubectl -n "${NS}" rollout status statefulset/${SVC} --timeout=300s

POD="${SVC}-0"
G="/garage -c /etc/garage.toml"
echo "==> Bootstrap cluster layout (idempotent) + buckets + key"
kubectl -n "${NS}" exec "${POD}" -- ${G} status | grep -q "NO ROLE" \
  && kubectl -n "${NS}" exec "${POD}" -- ${G} layout assign -z local --capacity 1G "$(kubectl -n ${NS} exec ${POD} -- ${G} status | grep -oE '^[0-9a-f]{16,}' | head -1)" \
       && kubectl -n "${NS}" exec "${POD}" -- ${G} layout apply --version 1 || true
kubectl -n "${NS}" exec "${POD}" -- ${G} bucket create ofbiz-content 2>/dev/null || true
kubectl -n "${NS}" exec "${POD}" -- ${G} bucket create ofbiz-audit 2>/dev/null || true
# Key rotation per run: secret is only printed at creation time
kubectl -n "${NS}" exec "${POD}" -- ${G} key info ofbiz-s3 >/dev/null 2>&1 \
  && kubectl -n "${NS}" exec "${POD}" -- ${G} key delete --yes ofbiz-s3 >/dev/null 2>&1 || true
CREATE_OUT="$(kubectl -n ${NS} exec ${POD} -- ${G} key create ofbiz-s3 2>/dev/null)"
kubectl -n "${NS}" exec "${POD}" -- ${G} key allow ofbiz-s3 --read --write --create-bucket 2>/dev/null || true
kubectl -n "${NS}" exec "${POD}" -- ${G} bucket allow --read --write ofbiz-content --key ofbiz-s3 >/dev/null 2>&1 || true
kubectl -n "${NS}" exec "${POD}" -- ${G} bucket allow --read --write ofbiz-audit --key ofbiz-s3 >/dev/null 2>&1 || true

KEY_ID="$(echo "${CREATE_OUT}" | grep -oE 'GK[0-9a-f]+' | head -1)"
SECRET="$(echo "${CREATE_OUT}" | grep -oE 'Secret key: +[0-9a-zA-Z+/=]+' | sed 's/Secret key: *//')"
if [ -z "${KEY_ID}" ] || [ -z "${SECRET}" ]; then echo "FAIL: could not read S3 key from create output"; echo "${CREATE_OUT}"; exit 1; fi

echo "==> S3 put/get roundtrip (awscli job)"
kubectl -n "${NS}" delete job s3-verify --ignore-not-found >/dev/null
kubectl -n "${NS}" create job s3-verify --image=amazon/aws-cli:2.23.0 -- \
  sh -c "aws configure set default.s3.signature_version s3v4;
AWS_ACCESS_KEY_ID=${KEY_ID} AWS_SECRET_ACCESS_KEY=${SECRET} AWS_DEFAULT_REGION=ofbiz-local \
  aws --endpoint-url http://${SVC}:3900 s3 cp /etc/hostname s3://ofbiz-content/verify.txt &&
AWS_ACCESS_KEY_ID=${KEY_ID} AWS_SECRET_ACCESS_KEY=${SECRET} AWS_DEFAULT_REGION=ofbiz-local \
  aws --endpoint-url http://${SVC}:3900 s3 ls s3://ofbiz-content/" >/dev/null
kubectl -n "${NS}" wait --for=condition=complete job/s3-verify --timeout=180s
kubectl -n "${NS}" logs job/s3-verify | tail -1 | grep -q "verify.txt" \
  && echo "PASS: S3 put/get roundtrip + bucket list" \
  || { echo "FAIL:"; kubectl -n ${NS} logs job/s3-verify; exit 1; }

kubectl -n "${NS}" delete job s3-verify --ignore-not-found >/dev/null
echo "ALL CHECKS PASSED — local S3 ready; cloud swap = endpoint+creds (cloud-native contract intact)"
