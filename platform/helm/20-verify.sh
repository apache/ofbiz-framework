#!/usr/bin/env bash
# P0-F4-S2 gate evidence: demo-svc healthy, HPA+PDB present, and a rolling
# upgrade serves traffic with zero failed requests.
set -euo pipefail

echo "== readiness =="
kubectl -n ofbiz get deploy,hpa,pdb -l app.kubernetes.io/name=demo-svc

echo "== /hello smoke =="
kubectl -n ofbiz run smoke --rm -i --restart=Never --image=curlimages/curl:8.10.1 --command -- \
  curl -sS -f --max-time 5 "http://demo-svc.ofbiz.svc.cluster.local:8080/hello?name=smoke"
echo

echo "== rolling update under traffic =="
kubectl -n ofbiz delete pod traffic-probe --force --grace-period=0 >/dev/null 2>&1 || true
kubectl -n ofbiz apply -f - >/dev/null <<'EOF'
apiVersion: v1
kind: Pod
metadata:
  name: traffic-probe
  labels: {app: traffic-probe}
spec:
  restartPolicy: Never
  containers:
    - name: curl
      image: curlimages/curl:8.10.1
      command: ["/bin/sh", "-c"]
      args:
        - |
          fails=0; total=0
          end=$(( $(date +%s) + 120 ))
          while [ "$(date +%s)" -lt "$end" ]; do
            total=$((total+1))
            curl -sS -f --max-time 5 "http://demo-svc.ofbiz.svc.cluster.local:8080/hello?name=roll" >/dev/null 2>&1 || fails=$((fails+1))
          done
          echo "TOTAL=$total FAILS=$fails"
EOF
kubectl -n ofbiz wait --for=condition=Ready pod/traffic-probe --timeout=90s

helm -n ofbiz upgrade demo-svc "$(dirname "$0")/service-chart" \
  --set image.repository=demo-svc --set image.tag=dev --set autoscaling.enabled=true \
  --reuse-values >/dev/null
kubectl -n ofbiz rollout status deploy/demo-svc --timeout=180s

RESULT=""
for _ in $(seq 1 150); do
  RESULT="$(kubectl -n ofbiz logs traffic-probe 2>/dev/null | grep 'TOTAL=' || true)"
  [ -n "$RESULT" ] && break
  sleep 2
done
kubectl -n ofbiz delete pod traffic-probe --force --grace-period=0 >/dev/null 2>&1 || true

echo "requests: $RESULT"
TOTAL="$(echo "$RESULT" | grep -o 'TOTAL=[0-9]*' | cut -d= -f2)"
FAILS="$(echo "$RESULT" | grep -o 'FAILS=[0-9]*' | cut -d= -f2)"
if [ -z "$FAILS" ] || [ "$FAILS" -ne 0 ] || [ -z "$TOTAL" ] || [ "$TOTAL" -lt 10 ]; then
  echo "FAIL: traffic errors or no traffic during rolling update ($RESULT)"
  exit 1
fi
echo "ALL CHECKS PASSED"
