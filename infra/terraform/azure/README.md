# Azure environments — UAT / XAT / PROD (future)

Skeleton only; nothing here is applied or wired to CI yet. Per-environment
tfvars:

| Env  | Nodes | VM size          | DNS zone           |
|------|-------|------------------|--------------------|
| uat  | 2     | Standard_D4ds_v5 | uat.ofbiz.<domain> |
| xat  | 3     | Standard_D4ds_v5 | xat.ofbiz.<domain> |
| prod | 5     | Standard_D8ds_v5 | ofbiz.<domain>     |

Each env gets its own tfvars file (`uat.tfvars` etc.) once subscription,
DNS delegation and the tfstate storage account exist. The in-cluster stack
is NOT terraform — after `az aks get-credentials`, run the shared Ansible
playbook (`infra/ansible/playbooks/dev-stack.yml`) against the new context;
cloud-profile deltas (LE issuer, real DNS, broker authz) are annotated in
the component scripts themselves.
