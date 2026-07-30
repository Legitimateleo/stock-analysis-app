# ── Deploy user ──────────────────────────────────────────────────
# Scoped to exactly what this project's pipeline uses: CodePipeline,
# CodeBuild, CodeStar Connections, ECS, ECR, ALB/VPC, Secrets Manager,
# S3 (state + pipeline artifacts), CloudWatch Logs, and IAM role
# creation LIMITED to roles prefixed "parallax-" (not full IAM admin).
#
# No CodeCommit, no CodeDeploy — this project uses GitHub as source
# and standard rolling ECS deploys, not CodeCommit/CodeDeploy
# blue-green.

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
        Sid    = "PipelineServices"
        Effect = "Allow"
        Action = [
          "codepipeline:*",
          "codebuild:*",
          "codestar-connections:*"
        ]
        Resource = "*"
      },
      {
        Sid    = "ContainerServices"
        Effect = "Allow"
        Action = [
          "ecs:*",
          "ecr:*"
        ]
        Resource = "*"
      },
      {
        Sid    = "Networking"
        Effect = "Allow"
        Action = [
          "ec2:*",
          "elasticloadbalancing:*"
        ]
        Resource = "*"
      },
      {
        Sid    = "SecretsAndConfig"
        Effect = "Allow"
        Action = [
          "secretsmanager:*",
          "logs:*"
        ]
        Resource = "*"
      },
      {
        Sid    = "StateAndArtifactBuckets"
        Effect = "Allow"
        Action = ["s3:*"]
        Resource = [
          "arn:aws:s3:::${var.project_name}-*",
          "arn:aws:s3:::${var.project_name}-*/*"
        ]
      },
      {
        # Scoped IAM — can manage roles/policies ONLY if their name
        # starts with the project prefix. This is the key line that
        # keeps this user from being full IAM admin.
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
      }
    ]
  })
}

# ── Read-only viewer (teammate) ─────────────────────────────────
# Uses AWS's own managed ReadOnlyAccess policy — broad, but genuinely
# read-only (no Allow on any create/update/delete verb anywhere in
# the account), and it's the standard, well-audited choice for "wants
# to see everything, change nothing."

resource "aws_iam_user" "viewer" {
  name = "${var.project_name}-viewer-${var.viewer_user_name}"
  tags = { Purpose = "Console viewing access only" }
}

resource "aws_iam_user_policy_attachment" "viewer_readonly" {
  user       = aws_iam_user.viewer.name
  policy_arn = "arn:aws:iam::aws:policy/ReadOnlyAccess"
}
