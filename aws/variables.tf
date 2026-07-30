variable "aws_region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "us-east-1"
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
  description = "Branch CodePipeline watches"
  type        = string
  default     = "feature/cloud-backend"
}

variable "finnhub_api_key" {
  description = "Finnhub API key — stored as a SecureString in SSM, never in state as plain resource config"
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

variable "deploy_user_name" {
  description = "Your name/handle, appended to the deploy IAM user's name"
  type        = string
}

variable "viewer_user_name" {
  description = "Teammate's name/handle, appended to the read-only IAM user's name"
  type        = string
}
