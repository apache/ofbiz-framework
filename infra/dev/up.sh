#!/usr/bin/env bash
# Dev environment driver: kind cluster (Terraform) + in-cluster stack (Ansible).
# Usage: sh infra/dev/up.sh [--skip-stack]   # --skip-stack = cluster only
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

echo "==> [1/2] Terraform: kind cluster"
terraform -chdir="${ROOT}/infra/terraform/dev" init -input=false >/dev/null
terraform -chdir="${ROOT}/infra/terraform/dev" apply -auto-approve -input=false

if [ "${1:-}" = "--skip-stack" ]; then
  echo "Cluster ready (stack skipped). Apply later with:"
  echo "  cd infra/ansible && ansible-playbook playbooks/dev-stack.yml"
  exit 0
fi

echo "==> [2/2] Ansible: in-cluster stack (Phase 0 components)"
cd "${ROOT}/infra/ansible"
ansible-playbook playbooks/dev-stack.yml

echo "Dev environment up. Evidence: playbook task outputs above."
