# Vault + Secrets Store CSI (P0-F1-S3, local profile)

Dev-mode Vault (root token `root`, in-memory storage) + Secrets Store CSI
driver + HashiCorp vault-csi-provider, authenticated via Kubernetes service
account auth.

```bash
sh platform/vault/10-install.sh   # idempotent
sh platform/vault/20-verify.sh    # proves the secret mounts in the test pod
```

## Secret rotation runbook

The CSI provider mounts secret values as files; mounted files do **not**
hot-reload. Rotation therefore has two halves:

1. **Rotate the value in Vault** (cloud profile: a real secret; here:
   `vault kv put secret/ofbiz/demo username=... password=...`). Nothing
   breaks — pods keep the previously mounted bytes.
2. **Propagate to pods.** Either `kubectl delete pod <pod>` (stateless
   services; the fresh mount pulls the new value), or — when a `Secret`
   sync is configured via `secretObjects` in the SecretProviderClass — the
   CSI driver's rotation reconciler (installed with `syncSecret.enabled=true`)
   updates the K8s Secret on the poll interval and apps reading env/Secret
   volumes pick it up on their next rollout.

Local profile caveat: dev-mode Vault loses all state on pod restart, so
10-install.sh re-seeds the fixture secret on every run.
