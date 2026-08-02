# gcp/providers.tf — main module.
#
# Unlike the bootstrap (which uses local state), this module stores its
# state remotely in the GCS bucket the bootstrap created. Because of
# that, `terraform init` here lands on the remote backend from the very
# first run — there is no `-migrate-state` step like the AWS side needed,
# since nothing was ever local to migrate.

terraform {
  required_version = ">= 1.5"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 7.0"
    }
  }

  # State lives in the bootstrap bucket. NOTE: a backend block can't use
  # variables — the bucket name has to be a literal string here. If the
  # state bucket is ever recreated under a different name, update this by
  # hand. `prefix = "gcp"` means the state object is gcp/default.tfstate
  # inside the bucket.
  backend "gcs" {
    bucket = "parallax-cs446-tfstate"
    prefix = "gcp"
  }
}

# project and region are pinned to the school project on purpose. Because
# GCP project IDs are globally unique, this module can only ever act on
# parallax-cs446 — the structural guard against building in the wrong
# place, same idea as the bootstrap. The ops scripts will layer an
# explicit `gcloud config` check on top of this later.
provider "google" {
  project = var.project_id
  region  = var.region
}
