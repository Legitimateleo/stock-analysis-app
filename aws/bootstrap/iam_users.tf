# Moved here from the main aws/ module so these two IAM users survive
# every `terraform destroy` of the app infrastructure (VPC/ECS/ALB/
# pipeline). Only bootstrap/ itself is never destroyed in normal use.

variable "deploy_user_name" {
  description = "Your name/handle, appended to the deploy IAM user's name"
  type        = string
}

variable "viewer_user_name" {
  description = "Teammate's name/handle, appended to the read-only IAM user's name"
  type        = string
}

# ── Deploy user ──────────────────────────────────────────────────
resource "aws_iam_user" "deploy" {
  name = "${var.project_name}-deploy-${var.deploy_user_name}"
  tags = { Purpose = "Terraform apply + pipeline management" }
}

resource "aws_iam_user_policy" "deploy" {
  name = "${var.project_name}-deploy-policy"
  user = aws_iam_user.deploy.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid      = "PipelineServices"
        Effect   = "Allow"
        Action   = ["codepipeline:*", "codebuild:*", "codestar-connections:*"]
        Resource = "*"
      },
      {
        Sid      = "ContainerServices"
        Effect   = "Allow"
        Action   = ["ecs:*", "ecr:*"]
        Resource = "*"
      },
      {
        Sid      = "Networking"
        Effect   = "Allow"
        Action   = ["ec2:*", "elasticloadbalancing:*"]
        Resource = "*"
      },
      {
        Sid      = "SecretsAndConfig"
        Effect   = "Allow"
        Action   = ["secretsmanager:*", "logs:*"]
        Resource = "*"
      },
      {
        Sid      = "StateAndArtifactBuckets"
        Effect   = "Allow"
        Action   = ["s3:*"]
        Resource = ["arn:aws:s3:::${var.project_name}-*", "arn:aws:s3:::${var.project_name}-*/*"]
      },
      {
        Sid    = "ScopedIAMForProjectRoles"
        Effect = "Allow"
        Action = [
          "iam:CreateRole", "iam:DeleteRole", "iam:GetRole",
          "iam:PutRolePolicy", "iam:DeleteRolePolicy", "iam:GetRolePolicy",
          "iam:AttachRolePolicy", "iam:DetachRolePolicy",
          "iam:PassRole", "iam:TagRole", "iam:ListRolePolicies",
          "iam:ListAttachedRolePolicies"
        ]
        Resource = "arn:aws:iam::*:role/${var.project_name}-*"
      },
      {
        Sid    = "ScopedIAMForProjectUsers"
        Effect = "Allow"
        Action = [
          "iam:CreateUser", "iam:DeleteUser", "iam:GetUser", "iam:TagUser",
          "iam:PutUserPolicy", "iam:DeleteUserPolicy", "iam:GetUserPolicy",
          "iam:AttachUserPolicy", "iam:DetachUserPolicy",
          "iam:ListUserPolicies", "iam:ListAttachedUserPolicies"
        ]
        Resource = "arn:aws:iam::*:user/${var.project_name}-*"
      },
      {
        Sid      = "OneTimeServiceLinkedRole"
        Effect   = "Allow"
        Action   = "iam:CreateServiceLinkedRole"
        Resource = "*"
        Condition = {
          StringEquals = { "iam:AWSServiceName" = "ecs.amazonaws.com" }
        }
      }
    ]
  })
}

# ── Read-only viewer (teammate) ─────────────────────────────────
resource "aws_iam_user" "viewer" {
  name = "${var.project_name}-viewer-${var.viewer_user_name}"
  tags = { Purpose = "Console viewing access only" }
}

resource "aws_iam_user_policy_attachment" "viewer_readonly" {
  user       = aws_iam_user.viewer.name
  policy_arn = "arn:aws:iam::aws:policy/ReadOnlyAccess"
}

output "deploy_user_name" {
  value = aws_iam_user.deploy.name
}

output "viewer_user_name" {
  value = aws_iam_user.viewer.name
}
