data "azurerm_client_config" "current" {}

resource "azurerm_key_vault" "main" {
  name                     = "${var.project_name}-kv-${random_string.acr_suffix.result}"
  location                 = azurerm_resource_group.main.location
  resource_group_name      = azurerm_resource_group.main.name
  tenant_id                = data.azurerm_client_config.current.tenant_id
  sku_name                 = "standard"
  enable_rbac_authorization = true # RBAC over legacy access policies — current Microsoft-recommended default

  # 0-day purge protection would block terraform destroy from actually
  # removing the vault; soft-delete purge is handled in providers.tf's
  # features block instead. Matches the same "make destroy actually
  # destroy" reasoning as ECR force_delete / S3 force_destroy.
  soft_delete_retention_days = 7
}

resource "azurerm_key_vault_secret" "finnhub_api_key" {
  name         = "FINNHUB-API-KEY" # Key Vault secret names can't contain underscores
  value        = var.finnhub_api_key
  key_vault_id = azurerm_key_vault.main.id
  depends_on   = [azurerm_role_assignment.admin_kv]
}

resource "azurerm_key_vault_secret" "polygon_api_key" {
  name         = "POLYGON-API-KEY"
  value        = var.polygon_api_key == "" ? "unset" : var.polygon_api_key
  key_vault_id = azurerm_key_vault.main.id
  depends_on   = [azurerm_role_assignment.admin_kv]
}

# You (the person running terraform apply) need write access to create
# the two secrets above in the first place.
resource "azurerm_role_assignment" "admin_kv" {
  scope                = azurerm_key_vault.main.id
  role_definition_name = "Key Vault Administrator"
  principal_id         = var.key_vault_admin_object_id
}

# Each container group's system-assigned managed identity gets read
# access to Key Vault for parity/audit purposes, even though (per the
# pivot note in container_instances.tf) the containers themselves
# currently get their secrets as direct env vars rather than a live
# Key Vault reference at runtime.
resource "azurerm_role_assignment" "backend_kv_read" {
  scope                = azurerm_key_vault.main.id
  role_definition_name = "Key Vault Secrets User"
  principal_id         = azurerm_container_group.backend.identity[0].principal_id
}

resource "azurerm_role_assignment" "frontend_kv_read" {
  scope                = azurerm_key_vault.main.id
  role_definition_name = "Key Vault Secrets User"
  principal_id         = azurerm_container_group.frontend.identity[0].principal_id
}
