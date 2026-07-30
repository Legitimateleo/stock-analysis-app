resource "aws_codebuild_project" "build" {
  name         = "${var.project_name}-build"
  service_role = aws_iam_role.codebuild.arn

  artifacts {
    type = "CODEPIPELINE"
  }

  environment {
    compute_type    = "BUILD_GENERAL1_SMALL"
    image           = "aws/codebuild/standard:7.0"
    type            = "LINUX_CONTAINER"
    privileged_mode = true # required for `docker build` inside CodeBuild

    environment_variable {
      name  = "AWS_ACCOUNT_ID"
      value = data.aws_caller_identity.current.account_id
    }
    environment_variable {
      name  = "AWS_REGION"
      value = var.aws_region
    }
    environment_variable {
      name  = "BACKEND_ECR_REPO"
      value = aws_ecr_repository.backend.name
    }
    environment_variable {
      name  = "FRONTEND_ECR_REPO"
      value = aws_ecr_repository.frontend.name
    }
  }

  source {
    type      = "CODEPIPELINE"
    buildspec = file("${path.module}/buildspec.yml")
  }
}
