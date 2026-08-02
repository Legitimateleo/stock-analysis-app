output "backend_url" {
  description = "Public URL for the backend API"
  value       = "http://${azurerm_container_group.backend.fqdn}:${var.backend_container_port}"
}

output "frontend_url" {
  description = "Public URL for the Streamlit dashboard — this is the one to open in a browser"
  value       = "http://${azurerm_container_group.frontend.fqdn}:${var.frontend_container_port}"
}

output "acr_login_server" {
  value = azurerm_container_registry.main.login_server
}

output "key_vault_name" {
  value = azurerm_key_vault.main.name
}

output "resource_group_name" {
  value = azurerm_resource_group.main.name
}
