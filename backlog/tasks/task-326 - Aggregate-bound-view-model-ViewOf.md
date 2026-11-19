---
id: TASK-326
title: Add aggregate-bound frontend view model (@ViewOf)
status: Done
assignee: []
created_date: '2026-11-17 10:00'
labels: [feature, rest, bff]
dependencies: []
priority: medium
---

## Description
<!-- SECTION:DESCRIPTION:BEGIN -->
Code-first, minimal-annotation design (see decision-005): one annotation binds a view record to an aggregate; the developer writes mapping in plain Java via `ViewAssembler` (batch read: joins, aggregation, enrichment) and `ViewCommands` (create/update/delete delegating to generated services). A default mapper is generated when view fields equal aggregate fields. Prefab generates controller, paging, tenant/security scoping, validation, transaction boundary, test client and mother. No string-based DSL. Removes the handwritten DTO/mapping/controller boilerplate shown in the TASK-320 prototypes. Rationale: decision-005. Complements TASK-170 and TASK-319; coordinate with TASK-127 (exposure strategies) and TASK-321 (versioned contracts).
<!-- SECTION:DESCRIPTION:END -->

## Analysis
- 2026-11-19: The task spans annotation-model, processor/plugin, generated HTTP and test artifacts, tenant/security behavior, docs, and the Kafka example. Start by tracing current plugin hooks and aggregate endpoint generation; prototype in the plugin layer before deciding whether a core processor hook is needed. Also review decision-005 and TASK-127/TASK-321 to preserve their intended contracts.
- 2026-11-19: `PrefabPlugin` hooks are not sufficient for standalone view records: the per-aggregate hooks receive only `ClassManifest` instances discovered from `@Aggregate`, while `PrefabProcessor` owns controller/service/test-client creation and does not discover arbitrary annotated types. A plugin using `writeAdditionalFiles` can inspect the round, but cannot add methods to the generated aggregate controller or generate a view-scoped controller/service/test client through the current public hooks. The first-class feature therefore needs processor-owned view discovery and writers; keep mapping and command behavior in typed Java extension interfaces.
- 2026-11-19: The accepted decision uses `@View` as a placeholder in examples, but this task's title and acceptance criteria explicitly name `@ViewOf`; implementation will follow the task contract. Read endpoints can reuse generated aggregate services (`getById` and paged `getList`), which apply existing tenant filtering before the view mapper runs. Endpoint exposure is not yet modeled; its type/operation override design belongs with TASK-127 and must not silently add an incompatible second strategy.
- 2026-11-19: Implemented aggregate and operation exposure as a visibility gate, distinct from TASK-127's `endpoints` transport selection. Full processor tests initially exposed a regression for polymorphic roots without a direct `@Aggregate`; inherited exposure now retains their historical public default.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Prototype via a PrefabPlugin first; document which plugin hooks were insufficient
- [x] #2 Unknown view fields or type mismatches against the aggregate produce actionable compiler errors
- [x] #3 A view generates mapper (default or via `ViewAssembler`), GET-by-id and paged GET-list endpoints, test client and mother
- [x] #3b Existing `@GetById`/`@GetList`/`@Create`/`@Update`/`@Delete` configure view endpoints (path, method, security); `ViewCommands` endpoints exist only for annotated methods, with validation and a transaction boundary
- [x] #3d Shorthands work in order: field-name match, view constructor or `from(Aggregate)`, then `ViewAssembler`; a form record with `toRequest()` makes a simple `@Create` body-free
- [x] #3e An aggregate can be internal (`exposure = INTERNAL` at aggregate level, overridable per operation): service and records are generated, but no controller method, OpenAPI entry or HTTP test client; default stays public and existing apps are unchanged; coordinate attribute design with TASK-127 `endpoints`
- [x] #3f Views over internal aggregates are the only HTTP surface and must declare their own security; tenant filtering still applies through the service
- [x] #3c Source rows are tenant/security scoped before the assembler runs; assemblers receive a batch, never a single item
- [x] #4 Aggregate-service tenant filtering applies to the view, including counts; view endpoints declare their own security rather than inheriting aggregate-controller security
- [x] #5 Several views of one aggregate coexist; the aggregate record is unchanged
- [x] #6 Documented in the developer guide and annotation reference; the Kafka example replaces the handwritten dashboard DTO where applicable
<!-- AC:END -->

## Implementation Notes

- Added `@ViewOf`, `Exposure`, `ViewAssembler`, `ViewContext` and `ViewCommands`; processor-owned writers discover view records, validate mapping contracts, and generate view services, HTTP controllers, mothers and test clients. Mapping resolves by component match, constructor/static factory, then batch assembler.
- View reads reuse generated aggregate services for paging, tenant filtering and counts, while view endpoints declare their own security. `ViewCommands` generates transactional operations only for annotated command methods. Type-level create forms can map through a public `toRequest()` method.
- Added operation and aggregate exposure gating without changing the existing public default; service and domain artifacts remain generated for internal aggregates. The exposure visibility gate is distinct from TASK-127's endpoint transport selection.
- Migrated the Kafka dashboard to `@ViewOf` and a batch `ChannelDashboardAssembler`; updated the developer guide, annotation reference, feature guide and decision-005.
- `mvn test` passed all 514 annotation-processor tests, including all 9 `ViewOfIntegrationTest` cases, and reached the Avro example. The remaining `SaleIntegrationTest` failed because the Podman/Docker connection reset while pulling `confluentinc/cp-schema-registry:8.4.0`; its integration test did not reach application assertions. Earlier targeted Kafka assembler tests passed (4 tests).
