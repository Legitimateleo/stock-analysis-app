# Run this ONCE, before the main module, with its own local state (a
# storage account can't store its own state before it exists). After
# this applies, the main module's `terraform init` can be pointed at
# it as a remote backend — see ../README.md.

terraform {
  required_version = ">= 1.5"
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.90"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
}

provider "azurerm" {
  features {}
}

variable "azure_location" {
  type    = string
  default = "East US"
}

variable "project_name" {
  type    = string
  default = "parallax"
}

resource "random_string" "state_suffix" {
  length  = 8
  special = false
  upper   = false
}

resource "azurerm_resource_group" "tfstate" {
  name     = "${var.project_name}-tfstate-rg"
  location = var.azure_location
}

resource "azurerm_storage_account" "tfstate" {
  # Storage account names: 3-24 chars, lowercase alphanumeric only,
  # globally unique across all of Azure — same class of constraint as
  # the AWS state bucket's bucket_prefix, hence the random suffix here.
  name                     = "${var.project_name}tfstate${random_string.state_suffix.result}"
  resource_group_name      = azurerm_resource_group.tfstate.name
  location                 = azurerm_resource_group.tfstate.location
  account_tier             = "Standard"
  account_replication_type = "LRS" # cheapest replication tier — fine for a class project's state file
  min_tls_version          = "TLS1_2"

  blob_properties {
    versioning_enabled = true # equivalent to the AWS state bucket's aws_s3_bucket_versioning
  }
}

resource "azurerm_storage_container" "tfstate" {
  name                  = "tfstate"
  storage_account_name  = azurerm_storage_account.tfstate.name
  container_access_type = "private"
}

output "resource_group_name" {
  value = azurerm_resource_group.tfstate.name
}

output "storage_account_name" {
  value = azurerm_storage_account.tfstate.name
}

output "container_name" {
  value = azurerm_storage_container.tfstate.name
}
