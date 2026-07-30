# AWS Terraform — Parallax Cloud Deployment

Provisions: VPC (2 public + 2 private subnets, 1 NAT gateway), 2 ECR
repos, ECS Fargate cluster with 2 services (backend/frontend, standard
rolling deploy — no CodeDeploy blue/green), 1 ALB with path-based
routing, IAM roles, SSM SecureString secrets, CodeBuild project, and a
CodePipeline (GitHub source → CodeBuild → 2 parallel ECS deploy
actions).

**I built this without access to a real AWS account or the Terraform
registry from my sandbox — `terraform validate`/`plan` has not been run
against it.** Review before applying, and expect to fix at least one
small thing on your first real `terraform plan`.

---

## Prerequisites

```bash
brew install terraform    # or your OS's equivalent
aws configure              # your AWS credentials, region
```

## Setup

```bash
cd aws
cp terraform.tfvars.example terraform.tfvars
# edit terraform.tfvars — your real GitHub org/repo, API keys, and
# your + your teammate's names for the two IAM users
```

### Step 0 — bootstrap the state bucket (one time only)

```bash
cd bootstrap
terraform init
terraform apply
terraform output state_bucket_name
cd ..
```

Copy that bucket name into the commented `backend "s3"` block in
`providers.tf`, uncomment it, then:

```bash
terraform init -migrate-state
```

This moves your state off your laptop and into S3 — worth doing before
anyone else on the team runs `terraform apply` too, so you're not
sharing a local state file over Slack/email.

```bash
terraform plan
```

Read the plan output before applying — it should show ~25-30 resources
to create, nothing to destroy. If it shows destroys on a first run,
stop and figure out why before continuing.

```bash
terraform apply
```

## After apply — one manual step

Terraform creates the GitHub connection but **cannot finish
authorizing it** — that requires a human clicking through a GitHub
OAuth popup:

```bash
terraform output codestar_connection_arn
```

Go to **Developer Tools → Settings → Connections** in the AWS console,
find the connection, click **Update pending connection**, and
authorize via GitHub. Only after this does the pipeline actually have
permission to read your repo.

## Setting up the two IAM users' credentials

Terraform creates the `aws_iam_user` resources and their permissions,
but deliberately does **not** create console passwords or access keys
— both would be written to Terraform state as plain text, which is a
real, well-documented risk (the same class of problem as a state file
landing in a git commit). Generate credentials for each user manually,
after apply:

```bash
terraform output deploy_user_name
terraform output viewer_user_name
```

**For the deploy user (you):** in the console, IAM → Users → your
deploy user → Security credentials → Create access key (for CLI/
Terraform use going forward) and/or set a console password if you
also want browser access.

**For the viewer user (your teammate):** IAM → Users → the viewer
user → Security credentials → console password only — she doesn't
need access keys since she's not running anything programmatically.
Send her the console sign-in URL (Account ID or alias + username +
whatever password you set) through your password manager, not Slack/
email in plain text — see `Credentials_and_Ports_Guide.md` for how
that's organized across your AWS accounts.

## Trigger the first deploy

Push any commit to `feature/cloud-backend`, or manually click
**Release change** on the pipeline in the console. Watch Source → Build
→ Deploy go green.

```bash
terraform output alb_dns_name
```

Open that URL — frontend at `/`, backend API at `/api/*`. Same test as
every other stage of this project: search a stock, confirm live data.

## Tearing down

```bash
terraform destroy
```

This is the actual advantage of Terraform over the manual console
runbook — one command removes everything (ALB, NAT gateway, ECS
services, ECR repos, IAM roles, the pipeline itself), rather than
hunting through a dozen console screens to make sure nothing's still
billing. Run this once you've got your screenshots and don't need the
environment live anymore — a NAT gateway alone bills by the hour
whether or not it's being used.

## Cost notes specific to this config

- NAT gateway: ~$0.045/hr + data processing — the single biggest
  "forgot to tear down" cost risk here
- ALB: ~$0.0225/hr + LCU usage
- Fargate: billed per vCPU/memory-second while tasks are running
- Everything else (ECR storage, CloudWatch Logs at 7-day retention,
  SSM parameters, CodeBuild minutes) is comparatively small

With $100 in credit, this should be very affordable for the time it
takes to deploy, screenshot, and tear down — the risk is exclusively
about leaving it running for days unattended.

## Files

```
providers.tf              — AWS provider + Terraform version pin
variables.tf               — inputs (region, project name, GitHub repo, API keys)
vpc.tf                      — VPC via terraform-aws-modules/vpc/aws
ecr.tf                      — 2 ECR repos + lifecycle policies
ssm.tf                      — API keys as SecureString parameters
iam.tf                      — 4 IAM roles (ECS execution, ECS task, CodeBuild, CodePipeline)
s3.tf                       — pipeline artifact bucket
alb.tf                      — security groups, ALB, target groups, path routing
ecs.tf                      — cluster, task defs, services
codestar_connection.tf      — GitHub connection (needs manual auth, see above)
codebuild.tf                — CodeBuild project
buildspec.yml                — what CodeBuild actually runs
codepipeline.tf              — the pipeline itself
outputs.tf                   — ALB URL, ECR URLs, connection ARN
terraform.tfvars.example     — copy to terraform.tfvars, fill in real values
```
