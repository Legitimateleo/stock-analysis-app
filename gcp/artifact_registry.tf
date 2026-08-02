# gcp/artifact_registry.tf
#
# A single Docker repository holding BOTH images. This is the idiomatic
# GCP shape and a real AWS-vs-GCP difference worth naming in the report:
# ECR needs one repository per image (your AWS side had parallax-backend
# and parallax-frontend as two separate repos), whereas an Artifact
# Registry repository is a namespace that holds many named images — here
# backend and frontend both live under the one "parallax" repo, pushed as
# parallax/backend and parallax/frontend.

resource "google_artifact_registry_repository" "parallax" {
  location      = var.region
  repository_id = var.project_name # "parallax"
  format        = "DOCKER"
  description   = "Container images for the Parallax backend and frontend"

  # NOTE: the AWS side had ECR lifecycle rules ("keep last 10 images") to
  # stop storage creeping up across pipeline runs. Deliberately omitted
  # here — image volume for a class project is tiny and the whole stack is
  # torn down after screenshots, so retention never becomes a cost factor.
  # An Artifact Registry `cleanup_policies` block is easy to add later if
  # this repo is ever kept long-term.
}
