# gcp/bootstrap/main.tf
#
# Run this ONCE, before the main gcp/ module, with its own LOCAL state.
# A bucket can't hold its own state before it exists — the same
# chicken-and-egg the AWS S3 bootstrap solves. After this applies, the
# main module's `backend "gcs"` block is pointed at the bucket created
# here and state migrates off your laptop.
#
# Mirrors aws/bootstrap/main.tf. Two things GCS does differently from S3,
# both in your favor:
#   1. Bucket names are globally unique, so the name is derived from the
#      (already globally unique) project ID instead of a random suffix.
#   2. The GCS Terraform backend does state locking natively — there is
#      no DynamoDB-lock-table equivalent to stand up here.

terraform {
  required_version = ">= 1.5"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 7.0"
    }
  }
}

# project is pinned to the variable below, NOT to ambient credentials.
# Because GCP project IDs are globally unique, this means Terraform can
# only ever act on parallax-cs446 — if your active credentials don't have
# access to it, you get a clean permission error instead of silently
# building in the wrong place. That's the structural guard against the
# GCP version of the wrong-account incident. (The ops scripts will add an
# explicit `gcloud config` check on top of this later.)
provider "google" {
  project = var.project_id
  region  = var.region
}

variable "project_id" {
  type        = string
  description = "GCP project ID — the guardrail anchor. Must match `gcloud config get-value project`."
  default     = "parallax-cs446"
}

variable "region" {
  type        = string
  description = "Region for the state bucket. Keep this the same region you deploy Cloud Run into later."
  default     = "us-central1"
}

resource "google_storage_bucket" "state" {
  name     = "${var.project_id}-tfstate"
  location = var.region

  # Modern default — disables legacy per-object ACLs.
  uniform_bucket_level_access = true

  # Recover a prior state if an apply ever corrupts or truncates it.
  versioning {
    enabled = true
  }

  # Belt-and-suspenders: never publicly reachable.
  public_access_prevention = "enforced"

  # No force_destroy on the STATE bucket on purpose — same reasoning as
  # the AWS bootstrap. You don't want an automated `terraform destroy` of
  # the wrong module to be able to wipe your state history.
  force_destroy = false
}

output "state_bucket_name" {
  value       = google_storage_bucket.state.name
  description = "Copy this into the backend \"gcs\" block of the main gcp/ module."
}
