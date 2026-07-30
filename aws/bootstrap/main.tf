# Run this ONCE, before the main module, with its own local state
# (a bucket can't store its own state before it exists). After this
# applies, the main module's `terraform init` can be pointed at it as
# a remote backend — see README.md.

terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "project_name" {
  type    = string
  default = "parallax"
}

resource "aws_s3_bucket" "state" {
  bucket_prefix = "${var.project_name}-tfstate-"
  # No force_destroy here on purpose — state is the one bucket you
  # don't want accidentally emptied by an automated `terraform destroy`
  # run against the wrong module.
}

resource "aws_s3_bucket_versioning" "state" {
  bucket = aws_s3_bucket.state.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_public_access_block" "state" {
  bucket                  = aws_s3_bucket.state.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

output "state_bucket_name" {
  value = aws_s3_bucket.state.bucket
}
