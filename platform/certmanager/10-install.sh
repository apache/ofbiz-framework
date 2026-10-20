#!/usr/bin/env bash
# Phase 0 / P0-F1-S2: cert-manager + Istio ingress + wildcard TLS for the
# local kind profile. Cloud profile swaps the CA issuer for Let's Encrypt
# staging (see IMPLEMENTATION.md AC annotations).
# Idempotent. Override versions via env: CERTMANAGER_VERSION.
set -euo pipefail

CERTMANAGER_VERSION="${CERTMANAGER_VERSION:-v1.18.2}"

# Wildcard domain for the local profile: sslip.io resolves
# <anything>.127.0.0.1.sslip.io to 127.0.0.1, and kind's extraPortMappings
# publish 443 on the host — no DNS delegation needed (cloud profile uses the
# delegated wildcard of the real domain).
WILDCARD="*.127.0.0.1.sslip.io"
echo "==> Wildcard ${WILDCARD} (host 443 → kind control-plane)"

echo "==> Installing cert-manager ${CERTMANAGER_VERSION}"
kubectl apply --server-side -f "https://github.com/cert-manager/cert-manager/releases/download/${CERTMANAGER_VERSION}/cert-manager.yaml" >/dev/null \
  || { echo "FATAL: cert-manager manifests not reachable"; exit 1; }
kubectl -n cert-manager wait deploy/cert-manager-webhook --for=condition=Available --timeout=300s \
  && kubectl -n cert-manager wait deploy/cert-manager --for=condition=Available --timeout=60s

echo "==> Bootstrapping self-signed CA issuer (ofbiz-ca)"
kubectl apply --server-side -f - <<'EOF'
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata: {name: ofbiz-ca-bootstrap}
spec: {selfSigned: {}}
---
apiVersion: cert-manager.io/v1
kind: Certificate
metadata: {name: ofbiz-root-ca, namespace: cert-manager}
spec:
  isCA: true
  commonName: ofbiz-local-root-ca
  secretName: ofbiz-root-ca
  duration: 8760h
  privateKey: {algorithm: ECDSA, size: 256}
  issuerRef: {name: ofbiz-ca-bootstrap, kind: ClusterIssuer}
---
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata: {name: ofbiz-ca}
spec:
  ca: {secretName: ofbiz-root-ca}
EOF
kubectl -n cert-manager wait certificate/ofbiz-root-ca --for=condition=Ready --timeout=120s

echo "==> Issuing wildcard certificate (short-lived: proves renewal cycle locally)"
kubectl apply -f - <<EOF
apiVersion: cert-manager.io/v1
kind: Certificate
metadata: {name: ofbiz-wildcard-tls, namespace: istio-system}
spec:
  dnsNames: ["${WILDCARD}"]
  secretName: ofbiz-wildcard-tls
  duration: 1h        # ponytail: 1h so the renewal cycle is observable locally
  renewBefore: 45m
  privateKey: {algorithm: ECDSA, size: 256}
  issuerRef: {name: ofbiz-ca, kind: ClusterIssuer}
EOF
kubectl -n istio-system wait certificate/ofbiz-wildcard-tls --for=condition=Ready --timeout=180s

echo "==> Granting the gateway credential-read RBAC (must exist before the
gateway pod starts — istiod caches denial decisions)"
kubectl apply -f - <<'EOF'
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata: {name: istio-ingressgateway-tls, namespace: istio-system}
rules:
  - apiGroups: [""]
    resources: [secrets]
    verbs: [get, list, watch]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata: {name: istio-ingressgateway-tls, namespace: istio-system}
roleRef: {apiGroup: rbac.authorization.k8s.io, kind: Role, name: istio-ingressgateway-tls}
subjects: [{kind: ServiceAccount, name: default, namespace: istio-system}]
EOF

echo "==> Exposing istio ingress gateway (NodePort 30080/30443)"
kubectl apply -f - <<'EOF'
apiVersion: v1
kind: Service
metadata: {name: istio-ingressgateway, namespace: istio-system}
spec:
  type: NodePort
  selector: {istio: ingressgateway}
  ports:
    - {name: http2, port: 80, targetPort: 8082, nodePort: 30080}
    - {name: https, port: 443, targetPort: 8443, nodePort: 30443}
---
apiVersion: apps/v1
kind: Deployment
metadata: {name: istio-ingressgateway, namespace: istio-system}
spec:
  replicas: 1
  selector: {matchLabels: {istio: ingressgateway}}
  template:
    metadata:
      labels: {istio: ingressgateway, sidecar.istio.io/inject: "true"}
      annotations:
        inject.istio.io/templates: gateway
    spec:
      # Pin to the ingress-ready node and bind host 80/443 directly:
      # kind's extraPortMappings publishes those ports on the macOS host.
      nodeSelector: {ingress-ready: "true"}
      tolerations:
        - {key: node-role.kubernetes.io/control-plane, operator: Exists, effect: NoSchedule}
      containers:
        - name: istio-proxy
          image: docker.io/istio/proxyv2:1.31.0
          args: ["proxy", "router", "--domain", "istio-system.svc.cluster.local"]
          env:
            - {name: ISTIO_META_ROUTER_MODE, value: "sni-dnat"}
          ports:
            - {name: http, containerPort: 8082, hostPort: 80}
            - {name: https, containerPort: 8443, hostPort: 443}
          resources: {requests: {cpu: 50m, memory: 128Mi}}
          securityContext:
            allowPrivilegeEscalation: false
            capabilities: {drop: [ALL]}
            readOnlyRootFilesystem: true
          volumeMounts:
            - {name: workload-socket, mountPath: /var/run/secrets/workload-spiffe-uds}
            - {name: credential-socket, mountPath: /var/run/secrets/credential-uds}
            - {name: workload-certs, mountPath: /var/run/secrets/workload-spiffe-credentials}
      volumes:
        - {name: workload-socket, emptyDir: {}}
        - {name: credential-socket, emptyDir: {}}
        - {name: workload-certs, emptyDir: {}}
EOF
kubectl -n istio-system wait deploy/istio-ingressgateway --for=condition=Available --timeout=300s

echo "==> Routing ${WILDCARD} → demo-svc"
# Gateway sits in istio-system (the ingressgateway's namespace) so istiod
# authorizes the TLS credential read — cross-namespace credentials are
# rejected by default.
kubectl apply -f - <<EOF
apiVersion: networking.istio.io/v1
kind: Gateway
metadata: {name: ofbiz-public, namespace: istio-system}
spec:
  selector: {istio: ingressgateway}
  servers:
    - port: {name: https, number: 443, protocol: HTTPS}
      tls: {mode: SIMPLE, credentialName: ofbiz-wildcard-tls}
      hosts: ["${WILDCARD}"]
---
apiVersion: networking.istio.io/v1
kind: VirtualService
metadata: {name: demo-svc, namespace: istio-system}
spec:
  hosts: ["${WILDCARD}"]
  gateways: [ofbiz-public]
  http:
    - route: [{destination: {host: demo-svc.ofbiz.svc.cluster.local, port: {number: 8080}}}]
EOF

echo "cert-manager + ingress ready. Wildcard: ${WILDCARD} on https://<anything>.127.0.0.1.sslip.io"
echo "Verify with platform/certmanager/20-verify.sh"
