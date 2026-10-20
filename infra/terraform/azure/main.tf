# Azure environments: UAT / XAT / PROD (skeleton — do not apply yet).
#
# Deliberately NOT wired into CI or the dev driver. When the Azure
# subscription + DNS delegation land:
#   1. az login / OIDC federated creds for CI
#   2. terraform init && terraform apply -var-file=uat.tfvars (etc.)
#   3. reuse infra/ansible/playbooks/dev-stack.yml unchanged (its kubectl/
#      helm calls follow the current kubeconfig context)
#
# Sizing below follows ADR-010 (K8s + Istio; STRICT mTLS comes from the
# ansible bootstrap, not TF).

terraform {
  required_version = ">= 1.9"
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.0"
    }
  }
  # Backend per environment — create containers first:
  #   az storage container create --name tfstate-uat ...
  backend "azurerm" {}
}

provider "azurerm" {
  features {}
  subscription_id = var.subscription_id
}

variable "subscription_id" {
  type = string
}

variable "env" {
  type    = string # uat | xat | prod
}

variable "location" {
  type    = string
  default = "westeurope"
}

variable "node_count" {
  type = number
}

variable "node_size" {
  type = string
}

variable "dns_zone" {
  description = "Azure DNS zone for the environment wildcard (delegated from the program domain)"
  type        = string
}

locals {
  name = "ofbiz-${var.env}"
}

resource "azurerm_resource_group" "rg" {
  name     = "${local.name}-rg"
  location = var.location
}

resource "azurerm_kubernetes_cluster" "aks" {
  name                = local.name
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  dns_prefix          = local.name

  # ADR-013: Java 21 services on virtual threads; default node pool.
  default_node_pool {
    name       = "system"
    node_count = var.node_count
    vm_size    = var.node_size
  }

  identity {
    type = "SystemAssigned"
  }

  network_profile {
    network_plugin = "azure"
    network_policy = "cilium"
  }

  tags = { env = var.env, program = "ofbiz-modernization" }
}

output "kubeconfig_command" {
  value = "az aks get-credentials --resource-group ${azurerm_resource_group.rg.name} --name ${azurerm_kubernetes_cluster.aks.name}"
}
