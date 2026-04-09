---
id: TASK-106
title: Generate Amazon AWS infrastructure as code
status: To Do
assignee: []
created_date: '2026-04-09 17:10'
labels:
  - "\U0001F4E6feature"
dependencies: []
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Prefab applications need to be deployed somewhere. Rather than leaving developers to hand-craft cloud infrastructure from scratch, Prefab should generate all the necessary Terraform modules to provision a production-ready Amazon Web Services environment tailored to the exact set of Prefab modules the application uses.

The annotation processor inspects the project's Prefab module dependencies and domain model at build time and emits a `terraform/aws/` directory containing fully parameterised, ready-to-apply Terraform configurations for every required AWS resource.

The guiding principle is Prefab's core philosophy: **start high, dive deep when you need to**. The generated Terraform is opinionated and complete enough to deploy immediately, yet structured so developers can extend or override individual modules without regenerating everything.

Supported resource types and their triggering conditions:

- **Amazon ECS on Fargate** — always generated; hosts the containerised Spring Boot application without managing servers
- **Amazon Elastic Container Registry (ECR)** — always generated; stores the application's Docker image
- **Amazon RDS for PostgreSQL** — generated when `prefab-postgres` is on the classpath
- **Amazon DocumentDB** (MongoDB-compatible) — generated when `prefab-mongodb` is on the classpath
- **Amazon SNS topics and SQS queues** — generated when `prefab-sns-sqs` is on the classpath; topic and queue names are derived from the `@Topic` / `@Queue` annotations in the domain model; dead-letter queues and redrive policies are generated automatically
- **Amazon MSK (Managed Streaming for Apache Kafka)** — generated when `prefab-kafka` is on the classpath
- **AWS Secrets Manager secrets** — generated for every secret the application needs (DB credentials, signing keys, etc.)
- **VPC, subnets, security groups, and NAT gateway** — generated to allow ECS tasks to reach RDS/DocumentDB privately while remaining outbound-connected
- **IAM roles and instance profiles** — generated with least-privilege policies for each resource (e.g. SNS publish, SQS consume, Secrets Manager read)
- **Application Load Balancer (ALB)** — generated when the application exposes public REST endpoints
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 The `prefab-terraform` Maven module is extended so that the AWS writer coexists with the GCP and Azure writers under the same artifact (or as a submodule) and all three are activated by feature flags / configuration
- [ ] #2 The AWS writer is wired into `PrefabProcessor` via the existing `Plugin` SPI, executing alongside all other annotation-processor plugins at compile time
- [ ] #3 A base `main.tf` / `variables.tf` / `outputs.tf` scaffolding is always generated under `target/generated-sources/terraform/aws/` regardless of which Prefab modules are present; the scaffolding includes the `aws` provider block and remote state backend configuration
- [ ] #4 An ECS cluster, task definition, and Fargate service Terraform resource are generated when at least one `@Aggregate`-annotated class is detected; the task definition is parameterised with the Docker image URL, CPU/memory, environment variables, and secrets from Secrets Manager
- [ ] #5 An ECR repository Terraform resource is generated alongside ECS to store the application's container image, with a lifecycle policy to retain only the last N images
- [ ] #6 An RDS PostgreSQL instance (Multi-AZ optional via variable) Terraform resource is generated when `prefab-postgres` is on the compile classpath; the master password is stored in Secrets Manager and injected into the ECS task as an environment variable
- [ ] #7 An Amazon DocumentDB cluster Terraform resource is generated when `prefab-mongodb` is on the compile classpath
- [ ] #8 One SNS topic and one SQS queue (with dead-letter queue and redrive policy) Terraform resource are generated per `@Topic`-annotated event class when `prefab-sns-sqs` is on the classpath; SNS-to-SQS subscription resources are also generated
- [ ] #9 An MSK cluster and broker configuration Terraform resource are generated when `prefab-kafka` is on the classpath; broker count and instance type are exposed as input variables
- [ ] #10 A VPC with public and private subnets across at least two availability zones, an internet gateway, NAT gateway, and security groups are generated; RDS, DocumentDB, and MSK resources are placed in private subnets; the ALB is placed in public subnets
- [ ] #11 An IAM role for ECS task execution and an IAM role for the ECS task itself are generated; inline policies are generated with the minimum required permissions for each resource present (e.g. `sns:Publish`, `sqs:ReceiveMessage`, `secretsmanager:GetSecretValue`, `ecr:GetAuthorizationToken`)
- [ ] #12 An Application Load Balancer with HTTPS listener (using ACM certificate) and target group Terraform resource are generated when at least one REST endpoint exists in the domain model
- [ ] #13 The generated Terraform passes `terraform validate` without errors
- [ ] #14 Unit tests for the AWS writer follow the pattern of existing plugin tests and assert the content of every generated `.tf` file against expected fixtures
- [ ] #15 An `examples/aws-terraform` module (or a dedicated test profile) demonstrates a full Prefab application with all supported modules enabled and verifies that `terraform plan` succeeds against the generated configuration
<!-- AC:END -->
