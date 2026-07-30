output "alb_dns_name" {
  description = "Public URL for the app — frontend at /, backend at /api/*"
  value       = "http://${aws_lb.main.dns_name}"
}

output "ecr_backend_repo_url" {
  value = aws_ecr_repository.backend.repository_url
}

output "ecr_frontend_repo_url" {
  value = aws_ecr_repository.frontend.repository_url
}

output "codestar_connection_arn" {
  description = "Go authorize this in the console after apply — Developer Tools > Settings > Connections"
  value       = aws_codestarconnections_connection.github.arn
}

output "codepipeline_name" {
  value = aws_codepipeline.main.name
}


