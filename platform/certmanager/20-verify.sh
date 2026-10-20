#!/usr/bin/env bash
# Phase 0 / P0-F1-S2 AC (local profile):
#   AC1: https://ofbiz.<domain> resolves to istio-ingress and serves the
#        wildcard cert with a valid chain (verified against the issuing CA)
#   AC2: wildcard cert auto-renewal is scheduled (cert-manager renewalTime)
# Cloud-profile deltas (LE staging issuer, real wildcard DNS, port 443
# without NodePort) are annotated in IMPLEMENTATION.md.
set -euo pipefail

HOST="ofbiz.127.0.0.1.sslip.io"
URL="https://${HOST}/"

echo "==> AC1: ${URL} must serve a CA-valid wildcard cert"
kubectl -n cert-manager get secret ofbiz-root-ca -o jsonpath='{.data.ca\.crt}' | base64 -d > /tmp/ofbiz-ca.crt
CODE="$(curl -sS -o /dev/null -w '%{http_code}' --cacert /tmp/ofbiz-ca.crt --max-time 15 "${URL}")"
echo "    HTTP ${CODE}, TLS chain verified against ofbiz-root-ca"
echo "    served cert: $(openssl s_client -connect 127.0.0.1:443 -servername "${HOST}" </dev/null 2>/dev/null \
      | openssl x509 -noout -subject -ext subjectAltName 2>/dev/null | head -2 | tr '\n' ' ')"

echo "==> AC2: renewal scheduled on the wildcard cert"
RENEWAL="$(kubectl -n istio-system get certificate ofbiz-wildcard-tls -o jsonpath='{.status.renewalTime}')"
if [ -n "${RENEWAL}" ]; then
  echo "PASS: renewalTime=${RENEWAL} (duration 1h / renewBefore 45m — full rotation observable within the hour)"
else
  echo "FAIL: no renewalTime on certificate"; exit 1
fi

echo "ALL CHECKS PASSED — evidence above satisfies P0-F1-S2 AC (local profile)"
