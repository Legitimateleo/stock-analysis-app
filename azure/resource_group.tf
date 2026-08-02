# Azure has no direct equivalent to "tag everything with Project=x" the
# way the AWS module does — instead, everything lives inside one
# Resource Group, which is itself the natural unit of `terraform destroy`.
# Deleting this resource group removes every child resource in one shot,
# same end result as AWS's ~28-resource `terraform destroy`, achieved
# structurally instead of by tag.
resource "azurerm_resource_group" "main" {
  name     = "${var.project_name}-rg"
  location = var.azure_location
}
