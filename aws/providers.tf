terraform {
  required_version = ">= 1.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # After running bootstrap/ once (creates the state bucket), uncomment
  # this block with the real bucket name from its output, then run
  # `terraform init -migrate-state` to move local state into S3.
  #
  # backend "s3" {
  #   bucket  = "parallax-tfstate-xxxxxxxx"
  #   key     = "aws/terraform.tfstate"
  #   region  = "us-east-1"
  #   encrypt = true
  # }
}

provider "aws" {
  region = var.aws_region
}
