#!/usr/bin/env bash
# Phase 0 / P0-F1-S3: Vault (dev) + Secrets Store CSI driver +
# HashiCorp vault CSI provider, wired with Kubernetes auth.
# Local profile: dev-mode Vault (root token "root", in-memory storage).
# Cloud profile: HA Vault with integrated storage + real unseal flow.
# Idempotent.
set -euo pipefail

VAULT_NS="vault"
NS="ofbiz"

echo "==> Installing Vault (dev mode) — charts fetched with curl to a local
file: helm repo add hangs on this host's IPv6 path, and URL-chart installs
trip over the broken local repo cache."
VAULT_CHART_URL="https://helm.releases.hashicorp.com/vault-0.34.1.tgz"
CSI_CHART_URL="https://kubernetes-sigs.github.io/secrets-store-csi-driver/charts/secrets-store-csi-driver-1.6.1.tgz"
curl -fsSL -o /tmp/vault-chart.tgz "${VAULT_CHART_URL}"
curl -fsSL -o /tmp/csi-chart.tgz "${CSI_CHART_URL}"
helm upgrade --install vault /tmp/vault-chart.tgz -n "${VAULT_NS}" --create-namespace \
  --set server.dev.enabled=true --set injector.enabled=false --set ui.enabled=false >/dev/null
kubectl -n "${VAULT_NS}" wait pod/vault-0 --for=condition=Ready --timeout=300s

echo "==> Installing Secrets Store CSI driver + vault provider"
helm upgrade --install csi /tmp/csi-chart.tgz \
  -n kube-system --set syncSecret.enabled=true >/dev/null
kubectl -n kube-system wait deploy/secrets-store-csi-driver --for=condition=Available --timeout=300s
kubectl -n "${VAULT_NS}" apply -f \
  https://raw.githubusercontent.com/hashicorp/vault-csi-provider/main/deployment/vault-csi-provider.yaml >/dev/null
kubectl -n "${VAULT_NS}" set env deploy/vault-csi-provider VAULT_ADDR="http://vault.${VAULT_NS}.svc:8200" >/dev/null
kubectl -n "${VAULT_NS}" wait deploy/vault-csi-provider --for=condition=Available --timeout=300s

echo "==> Configuring Kubernetes auth + test policy in Vault"
# Dev mode root token = "root".
VAULT="kubectl -n ${VAULT_NS} exec vault-0 -- vault"
${VAULT} auth enable kubernetes >/dev/null 2>&1 || true
${VAULT} write auth/kubernetes/config \
  kubernetes_host="https://\${KUBERNETES_SERVICE_HOST}:\${KUBERNETES_SERVICE_PORT}" \
  token_reviewer_jwt="$(kubectl -n ${VAULT_NS} exec vault-0 -- cat /var/run/secrets/kubernetes.io/serviceaccount/token)" \
  kubernetes_ca_cert=@/var/run/secrets/kubernetes.io/serviceaccount/ca.crt >/dev/null

${VAULT} secrets enable -path=secret kv-v2 >/dev/null 2>&1 || true
${VAULT} policy write ofbiz-demo - >/dev/null <<'EOF'
path "secret/data/ofbiz/*" { capabilities = ["read"] }
EOF

kubectl -n "${NS}" create serviceaccount vault-test --dry-run=client -o yaml | kubectl apply -f - >/dev/null
${VAULT} write auth/kubernetes/role/ofbiz-demo \
  bound_service_account_names=vault-test \
  bound_service_account_namespaces="${NS}" \
  policies=ofbiz-demo ttl=20m >/dev/null

# Synthetic fixture secret (values are placeholders, not real credentials).
${VAULT} kv put secret/ofbiz/demo username=demo-user password=demo-pass >/dev/null

echo "==> Creating SecretProviderClass + test pod"
kubectl -n "${NS}" apply -f - <<EOF
apiVersion: secrets-store.csi.x-k8s.io/v1
kind: SecretProviderClass
metadata: {name: vault-demo, namespace: ${NS}}
spec:
  provider: vault
  parameters:
    vaultAddress: "http://vault.${VAULT_NS}.svc:8200"
    vaultRole: ofbiz-demo
    secrets: |
      - secretPath: secret/data/ofbiz/demo
        args:
          - username
          - password
        path: ofbiz-demo
---
apiVersion: v1
kind: Pod
metadata:
  name: vault-csi-test
  namespace: ${NS}
  annotations: {sidecar.istio.io/inject: "false"}
spec:
  serviceAccountName: vault-test
  containers:
    - name: test
      image: curlimages/curl
      command: ["sleep", "3600"]
      volumeMounts:
        - {name: secrets, mountPath: /mnt/secrets, readOnly: true}
  volumes:
    - name: secrets
      csi:
        driver: secrets-store.csi.k8s.io
        readOnly: true
        volumeAttributes: {secretProviderClass: vault-demo}
EOF
kubectl -n "${NS}" wait pod/vault-csi-test --for=condition=Ready --timeout=180s

echo "Vault CSI ready. Verify with platform/vault/20-verify.sh"
