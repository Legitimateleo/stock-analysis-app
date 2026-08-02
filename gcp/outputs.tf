# gcp/outputs.tf

output "artifact_registry_repo" {
  description = "Docker image path prefix. Push images as <this>/backend and <this>/frontend."
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.parallax.repository_id}"
}
