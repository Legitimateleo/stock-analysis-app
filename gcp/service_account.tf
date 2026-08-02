# gcp/service_account.tf
#
# A dedicated runtime identity for the Cloud Run services, instead of the
# default compute service account. This is the least-privilege equivalent
# of the AWS ecs_task role: the container runs AS this SA, and the only
# thing it's granted is read access to the two secrets it needs.

resource "google_service_account" "cloudrun_runtime" {
  account_id   = "${var.project_name}-run"
  display_name = "Parallax Cloud Run runtime service account"
}

# Grant the runtime SA read access to each secret. Scoped per-secret
# (secret-level IAM), not project-wide — it can read these two secrets and
# nothing else.
resource "google_secret_manager_secret_iam_member" "finnhub_access" {
  secret_id = google_secret_manager_secret.finnhub_api_key.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloudrun_runtime.email}"
}

resource "google_secret_manager_secret_iam_member" "polygon_access" {
  secret_id = google_secret_manager_secret.polygon_api_key.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloudrun_runtime.email}"
}
