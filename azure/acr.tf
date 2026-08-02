# One registry, two repos (backend/frontend) distinguished by image
# name — ACR doesn't split into per-service "repository" resources the
# way ECR does; a single registry holds both as
# <registry>.azurecr.io/parallax-backend and .../parallax-frontend.
resource "azurerm_container_registry" "main" {
  name                = "${var.project_name}acr${random_string.acr_suffix.result}"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  sku                 = "Basic" # cheapest tier — fine for a class project's image volume
  admin_enabled       = true    # simplest auth path for App Service to pull; see README on tightening this to Managed Identity pull instead
}

# ACR names must be globally unique across all of Azure and are
# alphanumeric only (no hyphens) — same class of constraint as S3
# bucket names, hence the random suffix.
resource "random_string" "acr_suffix" {
  length  = 6
  special = false
  upper   = false
}

# Keep the last 10 images per repo, same policy as AWS's ECR lifecycle
# rule — requires ACR Tasks / the Premium SKU to *enforce* automatically,
# so on Basic this is a documented manual habit rather than an enforced
# policy. Flagging this honestly rather than silently dropping the
# control AWS had.
