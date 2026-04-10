---
id: TASK-118
title: Generate Google Cloud Platform infrastructure as code
status: To Do
assignee: []
created_date: '2026-04-09 17:10'
labels:
  - "\U0001F4E6feature"
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Prefab applications need to be deployed somewhere. Rather than leaving developers to hand-craft cloud infrastructure from scratch, Prefab should generate all the necessary Terraform modules to provision a production-ready Google Cloud Platform environment tailored to the exact set of Prefab modules the application uses.

The annotation processor inspects the project's Prefab module dependencies and domain model at build time and emits a `terraform/gcp/` directory containing fully parameterised, ready-to-apply Terraform configurations for every required GCP resource.

The guiding principle is Prefab's core philosophy: **start high, dive deep when you need to**. The generated Terraform is opinionated and complete enough to deploy immediately, yet structured so developers can extend or override individual modules without regenerating everything.

Supported resource types and their triggering conditions:

- **Cloud Run** — always generated; hosts the containerised Spring Boot application
- **Artifact Registry** — always generated; stores the application's Docker image
- **Cloud SQL (PostgreSQL)** — generated when `prefab-postgres` is on the classpath
- **Firestore** (Native mode) — generated when `prefab-mongodb` is on the classpath (MongoDB-compatible API)
- **Google Cloud Pub/Sub topics and subscriptions** — generated when `prefab-pubsub` is on the classpath; topic and subscription names are derived from the `@Topic` / `@Subscription` annotations in the domain model
- **Secret Manager secrets** — generated for every secret the application needs (DB credentials, signing keys, etc.)
- **VPC network and serverless VPC connector** — generated to allow Cloud Run to reach Cloud SQL privately
- **IAM service accounts and bindings** — generated with least-privilege roles for each resource
- **Cloud Load Balancing with HTTPS** — generated when the application exposes public REST endpoints

A new `prefab-terraform` Maven module contains the Terraform writer plugin. It hooks into the Prefab annotation processor pipeline via the existing `Plugin` SPI and writes files to `${project.build.directory}/generated-sources/terraform/gcp/`.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A new `prefab-terraform` Maven module is created and registered in the root `pom.xml`; it contains the GCP Terraform writer and all supporting classes
- [ ] #2 The GCP writer is wired into `PrefabProcessor` via the existing `Plugin` SPI, executing alongside all other annotation-processor plugins at compile time
- [ ] #3 A base `main.tf` / `variables.tf` / `outputs.tf` scaffolding is always generated under `target/generated-sources/terraform/gcp/` regardless of which Prefab modules are present
- [ ] #4 A Cloud Run service Terraform resource is generated when the annotation processor detects at least one `@Aggregate`-annotated class; the resource is parameterised with the Docker image URL, CPU/memory limits, and environment variables
- [ ] #5 An Artifact Registry repository Terraform resource is generated alongside Cloud Run to store the application's container image
- [ ] #6 A Cloud SQL (PostgreSQL) instance and database Terraform resource is generated when `prefab-postgres` is on the compile classpath; connection details are exposed as Secret Manager secrets and injected into the Cloud Run service as environment variables
- [ ] #7 A Firestore database Terraform resource is generated when `prefab-mongodb` is on the compile classpath
- [ ] #8 One Pub/Sub topic and one pull subscription Terraform resource are generated per `@Topic`-annotated event class when `prefab-pubsub` is on the classpath
- [ ] #9 A VPC network, subnet, and serverless VPC connector Terraform resource are generated whenever Cloud SQL or other private resources are included, so Cloud Run can reach them without a public IP
- [ ] #10 A dedicated IAM service account is generated for the Cloud Run service with the minimum set of IAM role bindings required by the resources present (e.g. `roles/cloudsql.client`, `roles/pubsub.publisher`, `roles/pubsub.subscriber`, `roles/secretmanager.secretAccessor`)
- [ ] #11 A Cloud Load Balancing (HTTPS) Terraform module is generated when at least one REST endpoint exists in the domain model
- [ ] #12 The generated Terraform passes `terraform validate` without errors
- [ ] #13 Unit tests for the GCP writer follow the pattern of existing plugin tests (e.g. `CreatePluginTest`) and assert the content of every generated `.tf` file against expected fixtures
- [ ] #14 An `examples/gcp-terraform` module (or a dedicated test profile) demonstrates a full Prefab application with all supported modules enabled and verifies that `terraform plan` succeeds against the generated configuration
<!-- AC:END -->
