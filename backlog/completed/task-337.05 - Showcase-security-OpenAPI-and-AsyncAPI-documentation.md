---
id: TASK-337.05
title: 'Showcase security, OpenAPI and AsyncAPI documentation'
status: Done
assignee: []
created_date: '2026-11-20 19:47'
updated_date: '2026-11-23 10:40'
labels:
  - examples
  - security
dependencies:
  - TASK-337.04
parent_task_id: TASK-337
priority: medium
type: task
ordinal: 176000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add prefab-security, prefab-openapi and prefab-async-api to the showcase. Replaces the scope of TASK-046.

Cover: `@Security` on endpoints (e.g. only admins edit the catalog, customers see only their own orders), `@CreatedBy`/`@LastModifiedBy` filled from the principal, generated OpenAPI with Swagger UI, generated AsyncAPI for the order events. Use a test-friendly auth setup; no secrets in config (env vars only, as today with GOOGLE_OAUTH2_*).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Integration tests cover allowed and forbidden access for at least two roles
- [x] #2 OpenAPI and AsyncAPI documents are served and checked by a test
- [x] #3 Application runs locally without real OAuth credentials
- [x] #4 Demonstrates default security, authority-based access and an endpoint with security disabled (scope carried over from TASK-046)
<!-- AC:END -->

## Analysis

The showcase module and its capability-driven integration-test setup already exist. This task must add
security behavior and contract-document endpoints to the shared showcase source so technology variants
exercise the same contracts. Keep authentication local and test-friendly, without requiring OAuth
credentials. Verify security annotations and audit principal support against the existing Prefab APIs,
then test the generated OpenAPI and AsyncAPI routes through the running application.

### Follow-up analysis

The generated customer-history list endpoint accepted a caller-supplied customer filter, so any authenticated user
could request another customer's history. The `Order` aggregate's default authenticated GET endpoints also exposed
orders without an owner check. Replace those reads with an admin-only order surface and a customer-history route
whose path customer ID must match the authenticated principal name. Keep the generated repository available for
event projection and test setup.

## Implementation Notes

- Added `prefab-security`, `prefab-openapi` and `prefab-async-api` to the showcase and shared variant parent, so
  variants compile the same security and documentation features.
- Product writes demonstrate the `ADMIN` role and `ROLE_ADMIN` authority; category listing demonstrates
  `@Security(enabled = false)` without bypassing the filter-chain policy.
- Order and overview reads are admin-only. Removed the generic customer-history REST list and added a route that
  checks the requested customer ID against the authenticated principal, preventing cross-customer history reads.
- Local and test profiles use HTTP Basic without configured OAuth credentials or checked-in passwords. The local
  profile grants the generated development user the showcase roles. Added an `AuditContextProvider` that records the
  active principal for audit fields.
- Added integration coverage for allowed and forbidden role access, customer-owned history, authentication defaults,
  the disabled endpoint, OpenAPI JSON and Swagger UI, AsyncAPI JSON, and principal-backed audit values.
- Updated `examples/README.md`, the feature guide and module matrix with the showcase behavior and documentation URLs.
- Verification: focused showcase integration tests passed (9 tests), and the MongoDB variant `test-compile` passed.
  The full root `mvn test` run was blocked by Testcontainers image-pull failures in legacy Avro, Kafka and SNS/SQS
  examples (registry connection reset/timeouts); the changed showcase tests passed independently.
