---
id: TASK-119
title: Generate Microsoft Azure infrastructure as code
status: To Do
assignee: []
created_date: '2026-04-09 17:10'
updated_date: '2026-04-24 06:57'
labels:
  - "\U0001F4E6feature"
dependencies: []
ordinal: 132000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Prefab applications need to be deployed somewhere. Rather than leaving developers to hand-craft cloud infrastructure from scratch, Prefab should generate all the necessary Terraform modules to provision a production-ready Microsoft Azure environment tailored to the exact set of Prefab modules the application uses.

The annotation processor inspects the project's Prefab module dependencies and domain model at build time and emits a `terraform/azure/` directory containing fully parameterised, ready-to-apply Terraform configurations for every required Azure resource.

The guiding principle is Prefab's core philosophy: **start high, dive deep when you need to**. The generated Terraform is opinionated and complete enough to deploy immediately, yet structured so developers can extend or override individual modules without regenerating everything.

Supported resource types and their triggering conditions:

- **Azure Container Apps** — always generated; hosts the containerised Spring Boot application with built-in HTTPS and auto-scaling
- **Azure Container Registry** — always generated; stores the application's Docker image
- **Azure Database for PostgreSQL Flexible Server** — generated when `prefab-postgres` is on the classpath
- **Azure Cosmos DB for MongoDB** — generated when `prefab-mongodb` is on the classpath
- **Azure Event Hubs namespace and hubs** — generated when `prefab-kafka` or the planned Event Hubs integration (TASK-023) is on the classpath; hub names are derived from the Kafka topic annotations in the domain model
- **Azure Key Vault secrets** — generated for every secret the application needs (DB connection strings, signing keys, etc.)
- **Azure Virtual Network and subnet delegation** — generated to allow Container Apps to reach the database privately
- **User-assigned Managed Identity** — generated with least-privilege role assignments for each resource, eliminating the need for stored credentials
- **Azure Application Gateway or Front Door** — generated when the application exposes public REST endpoints
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The GCP Terraform writer module (`prefab-terraform`) introduced in the GCP task is extended (or the Azure writer is added as a separate submodule) so that both GCP and Azure writers coexist under the same Maven artifact
- [ ] #2 The Azure writer is wired into `PrefabProcessor` via the existing `Plugin` SPI, executing alongside all other annotation-processor plugins at compile time
- [ ] #3 A base `main.tf` / `variables.tf` / `outputs.tf` scaffolding is always generated under `target/generated-sources/terraform/azure/` regardless of which Prefab modules are present; the scaffolding includes the `azurerm` provider block and a resource group resource
- [ ] #4 An Azure Container Apps environment and container app Terraform resource are generated when at least one `@Aggregate`-annotated class is detected; the resource is parameterised with the Docker image URL, CPU/memory, environment variables, and ingress configuration
- [ ] #5 An Azure Container Registry Terraform resource is generated alongside Container Apps to store the application's container image, together with the role assignment granting the Managed Identity `AcrPull` rights
- [ ] #6 An Azure Database for PostgreSQL Flexible Server Terraform resource is generated when `prefab-postgres` is on the compile classpath; connection details are stored in Key Vault and injected into the container app as environment variables via Key Vault references
- [ ] #7 An Azure Cosmos DB account (MongoDB API) Terraform resource is generated when `prefab-mongodb` is on the compile classpath
- [ ] #8 One Event Hubs namespace and one Event Hub Terraform resource are generated per Kafka topic annotation when `prefab-kafka` or the Event Hubs integration is on the classpath; consumer group resources are generated for each consumer group name found in the domain model
- [ ] #9 An Azure Virtual Network, subnet, and private endpoint are generated whenever a database or Event Hubs resource is included, so the Container App can reach them without a public IP
- [ ] #10 A user-assigned Managed Identity Terraform resource is generated and assigned to the Container App; role assignments are generated with the minimum required permissions (e.g. `Key Vault Secrets User`, `Azure Event Hubs Data Sender`, `Azure Event Hubs Data Receiver`)
- [ ] #11 The generated Terraform passes `terraform validate` without errors
- [ ] #12 Unit tests for the Azure writer follow the pattern of existing plugin tests and assert the content of every generated `.tf` file against expected fixtures
- [ ] #13 An `examples/azure-terraform` module (or a dedicated test profile) demonstrates a full Prefab application with all supported modules enabled and verifies that `terraform plan` succeeds against the generated configuration
<!-- AC:END -->
