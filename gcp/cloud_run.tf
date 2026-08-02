# gcp/cloud_run.tf
#
# Two Cloud Run services, both public (Option A). This single file
# replaces the AWS ECS-Fargate-behind-an-ALB layer entirely — no cluster,
# no task definitions, no ALB, no target groups, no VPC, no NAT. Each
# service gets its own managed HTTPS URL.
#
# Ordering is one-way: the frontend references the backend's URL, the
# backend references nothing (CORS is irrelevant — Streamlit calls the API
# server-side), so there's no dependency cycle to work around.

# ── Backend ──────────────────────────────────────────────────────
resource "google_cloud_run_v2_service" "backend" {
  name                = "${var.project_name}-backend"
  location            = var.region
  deletion_protection = false # class project — must stay destroyable
  ingress             = "INGRESS_TRAFFIC_ALL"

  template {
    service_account = google_service_account.cloudrun_runtime.email

    # min=1 keeps one instance warm. This matters: the frontend's HTTP
    # client times out at 10s (api_client.py TIMEOUT_SECONDS), and a cold
    # JVM start can exceed that — so a scaled-to-zero backend would fail
    # the teammate's very first click. One warm instance avoids it, and
    # it's cheap against the credit.
    scaling {
      min_instance_count = 1
      max_instance_count = 3
    }

    containers {
      image = "${var.region}-docker.pkg.dev/${var.project_id}/${var.project_name}/backend:latest"

      ports {
        container_port = var.backend_container_port
      }

      resources {
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }
      }

      # NOTE: no PORT env var here on purpose — Cloud Run injects PORT
      # automatically (equal to container_port), and the backend reads
      # ${PORT}. Setting PORT yourself is rejected by Cloud Run.

      env {
        name = "FINNHUB_API_KEY"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.finnhub_api_key.secret_id
            version = "latest"
          }
        }
      }

      env {
        name = "POLYGON_API_KEY"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.polygon_api_key.secret_id
            version = "latest"
          }
        }
      }
    }
  }

  # Secret versions must exist and the runtime SA must already have read
  # access before the service tries to mount them.
  depends_on = [
    google_secret_manager_secret_version.finnhub_api_key,
    google_secret_manager_secret_version.polygon_api_key,
    google_secret_manager_secret_iam_member.finnhub_access,
    google_secret_manager_secret_iam_member.polygon_access,
  ]
}

# Public invoker. Mirrors AWS, where the internet-facing ALB already
# exposed /api/* to anyone.
resource "google_cloud_run_v2_service_iam_member" "backend_public" {
  location = google_cloud_run_v2_service.backend.location
  name     = google_cloud_run_v2_service.backend.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}

# ── Frontend ─────────────────────────────────────────────────────
resource "google_cloud_run_v2_service" "frontend" {
  name                = "${var.project_name}-frontend"
  location            = var.region
  deletion_protection = false
  ingress             = "INGRESS_TRAFFIC_ALL"

  template {
    service_account = google_service_account.cloudrun_runtime.email

    scaling {
      min_instance_count = 0 # Streamlit cold start is quick and nothing downstream times out on it
      max_instance_count = 3
    }

    containers {
      image = "${var.region}-docker.pkg.dev/${var.project_id}/${var.project_name}/frontend:latest"

      # container_port 8501 makes Cloud Run route to Streamlit's hardcoded
      # port (Dockerfile: --server.port=8501) and set PORT=8501, so no
      # Dockerfile change is needed.
      ports {
        container_port = var.frontend_container_port
      }

      resources {
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }
      }

      # The reason for two services: the frontend is told where the
      # backend lives via its Cloud Run URL. Because Streamlit calls the
      # backend server-side, this cross-URL call needs no CORS and no
      # shared load balancer.
      env {
        name  = "API_BASE_URL"
        value = google_cloud_run_v2_service.backend.uri
      }
    }
  }

  depends_on = [google_cloud_run_v2_service.backend]
}

resource "google_cloud_run_v2_service_iam_member" "frontend_public" {
  location = google_cloud_run_v2_service.frontend.location
  name     = google_cloud_run_v2_service.frontend.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}
