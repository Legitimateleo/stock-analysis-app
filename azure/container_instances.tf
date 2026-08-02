# PIVOT NOTE (Challenges Encountered material): the original resource
# matrix called for App Service (see Phase2_Deployment_POA.md) for the
# PaaS-vs-orchestration contrast against AWS's ECS and GCP's Cloud Run.
# That plan hit a wall in the field: this subscription's App Service
# compute quota (Microsoft.Web) was provisioned at 0 across every SKU
# (F1/D1/B1/B2/B3/S1/S2/S3) and every region tried, independent of the
# separate Microsoft.Compute vCPU quota (confirmed fine at 4/4 via
# `az vm list-usage`). Two rounds of self-service quota-increase
# requests were rejected outright ("We were unable to adjust your
# quota"), even after upgrading the subscription from Free Trial to
# Pay-As-You-Go specifically to try to clear it. Rather than block the
# whole deployment on an open-ended support ticket, we pivoted to Azure
# Container Instances (ACI) — a different Azure resource type backed
# by a completely separate quota pool (Microsoft.ContainerInstance),
# which was unaffected.
#
# This is a genuine architecture deviation from the original resource
# matrix, not a config tweak — worth reporting honestly in section 5.5
# (Challenges Encountered) rather than writing the report as if ACI
# was the plan from the start. It does still deliver a real, if
# different, PaaS-vs-orchestration comparison point: ACI is Azure's
# "run a container, no cluster, no VM management" answer, closer in
# spirit to Cloud Run's simplicity than to App Service's web-app
# framework — arguably makes for an even more direct 3-way container
# comparison than the original plan did.

resource "azurerm_container_group" "backend" {
  name                = "${var.project_name}-backend-${random_string.acr_suffix.result}"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  os_type             = "Linux"
  restart_policy      = "Always"
  ip_address_type     = "Public"
  dns_name_label      = "${var.project_name}-backend-${random_string.acr_suffix.result}"

  identity {
    type = "SystemAssigned"
  }

  image_registry_credential {
    server   = azurerm_container_registry.main.login_server
    username = azurerm_container_registry.main.admin_username
    password = azurerm_container_registry.main.admin_password
  }

  container {
    name   = "backend"
    image  = "${azurerm_container_registry.main.login_server}/${var.project_name}-backend:latest"
    cpu    = "0.5"
    memory = "1.0"

    ports {
      port     = var.backend_container_port
      protocol = "TCP"
    }

    environment_variables = {
      PORT = tostring(var.backend_container_port)
    }

    # ACI has no equivalent of App Service's native
    # @Microsoft.KeyVault() reference syntax, so secrets are injected
    # directly as secure env vars from the Terraform variables here —
    # a real simplification versus the Key Vault-referenced approach
    # the App Service plan would have used. The Key Vault + secrets
    # this module already created still stand as the "Secrets"
    # resource-matrix line item and hold the same values for reference/
    # audit purposes; ACI just doesn't read from them at container
    # startup the way App Service would have.
    secure_environment_variables = {
      FINNHUB_API_KEY = var.finnhub_api_key
      POLYGON_API_KEY = var.polygon_api_key == "" ? "unset" : var.polygon_api_key
    }
  }

  lifecycle {
    ignore_changes = [container[0].image]
  }
}

resource "azurerm_container_group" "frontend" {
  name                = "${var.project_name}-frontend-${random_string.acr_suffix.result}"
  resource_group_name = azurerm_resource_group.main.name
  location            = azurerm_resource_group.main.location
  os_type             = "Linux"
  restart_policy      = "Always"
  ip_address_type     = "Public"
  dns_name_label      = "${var.project_name}-frontend-${random_string.acr_suffix.result}"

  identity {
    type = "SystemAssigned"
  }

  image_registry_credential {
    server   = azurerm_container_registry.main.login_server
    username = azurerm_container_registry.main.admin_username
    password = azurerm_container_registry.main.admin_password
  }

  container {
    name   = "frontend"
    image  = "${azurerm_container_registry.main.login_server}/${var.project_name}-frontend:latest"
    cpu    = "0.5"
    memory = "1.0"

    ports {
      port     = var.frontend_container_port
      protocol = "TCP"
    }

    environment_variables = {
      # ACI container groups get their own DNS name + public IP
      # directly — no shared load balancer/routing layer the way
      # App Service or the AWS ALB provided. Each service is reachable
      # at its own <dns_name_label>.<region>.azurecontainer.io:<port>.
      API_BASE_URL = "http://${azurerm_container_group.backend.fqdn}:${var.backend_container_port}"
    }
  }

  lifecycle {
    ignore_changes = [container[0].image]
  }

  depends_on = [azurerm_container_group.backend]
}
