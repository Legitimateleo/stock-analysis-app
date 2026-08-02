# Azure Terraform — Parallax Cloud Deployment

Provisions: 1 Resource Group, 1 VNet + subnet (App Service VNet
Integration — outbound only, see `vnet.tf` for why this is
intentionally thinner than AWS's VPC), 1 Azure Container Registry, 1
Key Vault (2 API-key secrets + 1 ACR admin credential note), 1 App
Service Plan (B1, Linux) hosting 2 Web Apps in container mode
(backend/frontend), System-Assigned Managed Identity on each Web App,
RBAC role assignments for Key Vault access.

**I built this without access to a real Azure subscription or the
Terraform registry from my sandbox — `terraform validate`/`plan` has
not been run against it.** Review before applying, and expect to fix
at least one small thing on your first real `terraform plan` — same
caveat the AWS README gave, and it turned out to be true there too.

---

## Prerequisites

```bash
brew install terraform azure-cli   # or your OS's equivalent
az login
az account show                     # confirm which subscription is active — see below
```

## Step 0 — confirm your subscription and credit balance FIRST

This was flagged in the handoff doc as the thing to check before
touching anything, and it still hasn't been confirmed:

```bash
az account show
az account list --output table      # if you have more than one
```

For remaining credit, `az` doesn't surface it directly — check
**portal.azure.com → Cost Management + Billing → Subscriptions** (or
**Cost Management → Overview** for non-student offers). AWS had $100,
GCP has $300 — until this number is known, scope the Azure work
conservatively (this module's B1 Plan is already the cheapest SKU that
supports custom containers).

Once you know which subscription is correct, hard-code its ID into
`scripts/start.sh` and `scripts/stop.sh` (`EXPECTED_SUBSCRIPTION_ID`)
before running either — they will refuse to run with the placeholder
left in place, on purpose.

## Setup

```bash
cd azure
cp terraform.tfvars.example terraform.tfvars
# edit terraform.tfvars — your real GitHub org/repo, API keys, and
# your own Azure AD object id (az ad signed-in-user show --query id -o tsv)
```

### Step 1 — bootstrap the state storage account (one time only)

```bash
cd bootstrap
terraform init
terraform apply
terraform output storage_account_name
terraform output resource_group_name
cd ..
```

Copy those values into the commented `backend "azurerm"` block in
`providers.tf`, uncomment it, then:

```bash
terraform init -migrate-state
```

This moves state off your laptop and into Blob Storage — same reason
as the AWS S3 backend migration: don't share a local state file over
Slack/email once a teammate might also run `terraform apply`.

```bash
terraform plan
```

Read the plan before applying — it should show resource creates only,
nothing to destroy, on a first run.

```bash
terraform apply
```

## After apply — wiring up GitHub Actions (the manual step)

Unlike AWS, Terraform does **not** create the CI/CD connection at all
here — per D8, Azure's CI/CD is App Service's built-in GitHub Actions
deployment via Deployment Center, which auto-generates its own
workflow file and commits it to your repo. That step needs your GitHub
credentials interactively, the same reason the AWS CodeStar connection
needed a manual console click after `terraform apply`:

```bash
az webapp deployment github-actions add \
  --resource-group parallax-rg \
  --name <backend-app-name-from-terraform-output> \
  --repo "Legitimateleo/stock-analysis-app" \
  --branch feature/cloud-backend \
  --login-with-github

az webapp deployment github-actions add \
  --resource-group parallax-rg \
  --name <frontend-app-name-from-terraform-output> \
  --repo "Legitimateleo/stock-analysis-app" \
  --branch feature/cloud-backend \
  --login-with-github
```

This opens a device-login flow in your browser, then commits
`.github/workflows/<app-name>.yml` directly to the repo — review that
file once it lands; it will build from the Dockerfile in the repo root
by default, so it likely needs a `working-directory`/build-context
edit to point at `backend/` and `frontend/` respectively, mirroring
what `buildspec.yml` does explicitly on the AWS side.

## Trigger the first deploy

Push any commit to `feature/cloud-backend`, or trigger the workflow
manually from the **Actions** tab on GitHub. Watch it build, push to
ACR, and deploy.

```bash
terraform output frontend_url
terraform output backend_url
```

Open the frontend URL — same test as every other stage of this
project: search a stock, confirm live data.

## Tearing down

```bash
terraform destroy
```

One command removes the Resource Group and everything in it — App
Service Plan, both Web Apps, ACR, Key Vault, VNet. Run this once
you've got your screenshots.

## Cost notes specific to this config

- **App Service Plan (B1):** billed hourly for as long as it *exists*,
  regardless of whether the apps are actively serving traffic — there
  is no Fargate-style per-task-second billing to scale to zero. This
  is the real reason `scripts/` has no `pause.sh`/`resume.sh` here:
  AWS's pause script scaled ECS tasks to 0 while the NAT Gateway/ALB
  kept running at a lower combined cost; Azure has nothing equivalent
  to pause, because the Plan **is** the cost, not a separate layer on
  top of compute. Worth naming as a genuine three-way difference in
  the comparison section, not a gap in this module.
- **ACR (Basic):** flat low monthly cost, storage-based.
- **Key Vault:** billed per operation, negligible at this volume.
- Real numbers go in `AZURE_Cost_Estimate.md` once resource
  configurations are final — replicate the AWS cost doc's two-method
  approach (manual calc + Azure Pricing Calculator at
  calculator.azure.com), not just one number.

## Giving a teammate read-only access

Azure doesn't need a separate Terraform-managed IAM user for this the
way AWS did — Azure RBAC role assignments on the Resource Group cover
it directly, scoped after the resource group exists:

```bash
az role assignment create \
  --assignee <teammate's email or Azure AD object id> \
  --role "Reader" \
  --resource-group parallax-rg
```

They'll be able to view every resource in the console but not modify
anything — same intent as AWS's `parallax-viewer-*` IAM user with
`ReadOnlyAccess`, achieved without a dedicated Terraform resource here.

## Tightening ACR auth (optional, not done here)

This module uses ACR's admin username/password (simplest path,
`admin_enabled = true`). The more-correct-for-production approach
skips admin credentials and grants each Web App's managed identity the
`AcrPull` role directly — flagging this as a known simplification
rather than silently presenting it as best practice.

## Files

```
providers.tf     — azurerm/random provider config + Terraform version pin
variables.tf      — inputs (location, project name, GitHub repo, API keys, your AD object id)
resource_group.tf  — the single destroy-everything container
vnet.tf              — VNet + subnet + App Service VNet Integration (see note on scope)
acr.tf                — Container Registry (Basic SKU)
key_vault.tf           — Key Vault, 2 API-key secrets, RBAC role assignments
app_service.tf           — Plan + 2 Linux Web Apps (container mode), managed identity, app settings
outputs.tf                — frontend/backend URLs, ACR login server, Key Vault name
terraform.tfvars.example   — copy to terraform.tfvars, fill in real values
bootstrap/                  — one-time state storage account + container (run first)
```
