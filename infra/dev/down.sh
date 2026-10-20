#!/usr/bin/env bash
# Tear the dev environment down: terraform destroy runs the null_resource
# destroy provisioner (kind delete cluster).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
terraform -chdir="${ROOT}/infra/terraform/dev" destroy -auto-approve -input=false
echo "Dev environment down."
