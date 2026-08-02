variable "azure_location" {
  description = "Azure region to deploy into"
  type        = string
  default     = "East US"
}

variable "project_name" {
  description = "Short name used to prefix all resources"
  type        = string
  default     = "parallax"
}

variable "github_owner" {
  description = "GitHub org/user that owns the repo, e.g. Legitimateleo"
  type        = string
}

variable "github_repo" {
  description = "GitHub repo name, e.g. stock-analysis-app"
  type        = string
}

variable "github_branch" {
  description = "Branch the App Service GitHub Actions workflow watches"
  type        = string
  default     = "feature/cloud-backend"
}

variable "finnhub_api_key" {
  description = "Finnhub API key — stored in Key Vault, never in state as plain resource config"
  type        = string
  sensitive   = true
}

variable "polygon_api_key" {
  description = "Polygon API key — optional, chart endpoint degrades gracefully without it"
  type        = string
  sensitive   = true
  default     = ""
}

variable "backend_container_port" {
  type    = number
  default = 8080
}

variable "frontend_container_port" {
  type    = number
  default = 8501
}

variable "key_vault_admin_object_id" {
  description = "Your own Azure AD object ID (az ad signed-in-user show --query id -o tsv) — gets Key Vault Administrator so you can read/write secrets during setup"
  type        = string
}
