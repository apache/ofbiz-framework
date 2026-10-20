# Dev infrastructure: local kind cluster as code (ADR-010 local profile).
#
# Terraform owns the CLUSTER lifecycle (create/destroy with state); the
# in-cluster stack (Istio, cert-manager, Kafka, Vault, observability,
# storage) is owned by the Ansible playbook invoked from infra/dev/up.sh —
# terraform deliberately stops at "kubectl get nodes is green".
#
# Azure environments (UAT/XAT/PROD) live in ../azure and replace this
# module with azurerm; the Ansible layer is reused unchanged.

terraform {
  required_version = ">= 1.9"
  required_providers {
    null = {
      source  = "hashicorp/null"
      version = "~> 3.2"
    }
  }
}

variable "cluster_name" {
  description = "kind cluster name (also the kubectl context suffix)"
  type        = string
  default     = "ofbiz"
}

variable "worker_count" {
  description = "extra worker nodes beyond the control-plane"
  type        = number
  default     = 2
}

resource "null_resource" "kind_cluster" {
  triggers = {
    cluster_name = var.cluster_name
    workers      = var.worker_count
    # recreate when the cluster definition changes
    config_sha = filesha256("${path.module}/kind.yaml.tpl")
  }

  provisioner "local-exec" {
    command = <<-EOT
      set -e
      kind get clusters | grep -qx '${var.cluster_name}' && exit 0
      sed -e 's/__CLUSTER_NAME__/${var.cluster_name}/g' \
          -e 's/__WORKER_REPLICAS__/${var.worker_count}/g' \
          '${path.module}/kind.yaml.tpl' > /tmp/${var.cluster_name}-kind.yaml
      kind create cluster --config /tmp/${var.cluster_name}-kind.yaml
    EOT
  }

  provisioner "local-exec" {
    when    = destroy
    command = "kind delete cluster --name ${self.triggers.cluster_name} || true"
  }
}

output "cluster_name" {
  value = var.cluster_name
}

output "kubeconfig_context" {
  value = "kind-${var.cluster_name}"
}
