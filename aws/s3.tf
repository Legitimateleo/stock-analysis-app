resource "aws_s3_bucket" "pipeline_artifacts" {
  bucket_prefix = "${var.project_name}-pipeline-artifacts-"
  force_destroy = true # so `terraform destroy` doesn't get blocked by leftover objects
}

resource "aws_s3_bucket_public_access_block" "pipeline_artifacts" {
  bucket                  = aws_s3_bucket.pipeline_artifacts.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
