# infra/ — environment provisioning (Terraform + Ansible)

| Path | Owns |
|---|---|
| `dev/up.sh`, `dev/down.sh` | driving scripts — bring Dev up/down |
| `terraform/dev/` | local kind cluster lifecycle (state-tracked) |
| `terraform/azure/` | UAT/XAT/PROD skeletons (not applied yet) |
| `ansible/playbooks/dev-stack.yml` | in-cluster stack on any kubeconfig context |

## Dev (local)

```bash
sh infra/dev/up.sh     # terraform apply (kind) → ansible-playbook (stack)
sh infra/dev/down.sh   # terraform destroy → kind delete cluster
```

Division of responsibility: Terraform stops at a green cluster; Ansible
installs the Phase 0 stack by reusing the idempotent `platform/*/` scripts
(single source of truth for component installs — no duplication). Re-run
single components with tags:

```bash
cd infra/ansible
ansible-playbook playbooks/dev-stack.yml --tags kafka
```

## Azure (later)

See `terraform/azure/README.md`.
