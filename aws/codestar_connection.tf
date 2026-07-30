resource "aws_codestarconnections_connection" "github" {
  name          = "${var.project_name}-github"
  provider_type = "GitHub"
}

# NOTE: Terraform can create this resource, but it starts in status
# "PENDING" — AWS requires a human to click "Authorize" in a GitHub
# OAuth popup in the console before the pipeline can actually use it.
# After `terraform apply`, go to:
#   Developer Tools → Settings → Connections
# and complete the authorization once. This can't be scripted.
