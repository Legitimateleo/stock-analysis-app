# gcp/outputs.tf

output "artifact_registry_repo" {
  description = "Docker image path prefix. Push images as <this>/backend and <this>/frontend."
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.parallax.repository_id}"
}

output "backend_url" {
  description = "Backend Cloud Run URL. Health check: <this>/actuator/health"
  value       = google_cloud_run_v2_service.backend.uri
}

output "frontend_url" {
  description = "THE LINK — the public Streamlit dashboard. Send this to your teammate."
  value       = google_cloud_run_v2_service.frontend.uri
}
