# gcp/variables.tf
#
# Foundational + app variables. Sensitive values (API keys) are supplied
# via a gitignored terraform.tfvars — never committed, never hardcoded.

variable "project_id" {
  type        = string
  description = "GCP project ID — the guardrail anchor. Must match `gcloud config get-value project`."
  default     = "parallax-cs446"
}

variable "project_name" {
  type        = string
  description = "Short name used to prefix resource names (repos, services, secrets)."
  default     = "parallax"
}

variable "region" {
  type        = string
  description = "Region for all GCP resources. Must match the state bucket's region."
  default     = "us-central1"
}

variable "finnhub_api_key" {
  type        = string
  description = "Finnhub API key — required. Stored in Secret Manager, supplied via gitignored terraform.tfvars."
  sensitive   = true
}

variable "polygon_api_key" {
  type        = string
  description = "Polygon API key — optional. Chart endpoint degrades gracefully to empty without it."
  sensitive   = true
  default     = ""
}

variable "backend_container_port" {
  type        = number
  description = "Port the backend listens on. Cloud Run injects this as the PORT env var."
  default     = 8080
}

variable "frontend_container_port" {
  type        = number
  description = "Port Streamlit listens on. Set as the Cloud Run container port so PORT matches the Dockerfile's 8501."
  default     = 8501
}
