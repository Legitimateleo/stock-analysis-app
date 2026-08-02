# gcp/secrets.tf
#
# Mirrors aws/secrets.tf. One difference: GCP secret IDs can't contain a
# "/", so where AWS used "parallax/FINNHUB_API_KEY" we use
# "parallax-FINNHUB_API_KEY". Same idea otherwise — the real values come
# from the gitignored terraform.tfvars, and Polygon degrades to a literal
# "unset" when blank (PolygonClient handles that gracefully, chart returns
# empty — decision D4).

resource "google_secret_manager_secret" "finnhub_api_key" {
  secret_id = "${var.project_name}-FINNHUB_API_KEY"
  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "finnhub_api_key" {
  secret      = google_secret_manager_secret.finnhub_api_key.id
  secret_data = var.finnhub_api_key
}

resource "google_secret_manager_secret" "polygon_api_key" {
  secret_id = "${var.project_name}-POLYGON_API_KEY"
  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "polygon_api_key" {
  secret      = google_secret_manager_secret.polygon_api_key.id
  secret_data = var.polygon_api_key == "" ? "unset" : var.polygon_api_key
}
