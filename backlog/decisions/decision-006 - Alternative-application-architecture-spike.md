---
id: decision-006
title: Keep aggregate generation as the only built-in application architecture
date: '2026-11-18'
status: accepted
related_tasks:
  - TASK-309
  - TASK-127
  - TASK-180
---

## Context

TASK-309 asked whether Prefab should generate an additional application architecture for workflows that do not map
cleanly to one aggregate. The representative case is importing a batch of products: validate rows, persist the valid
ones, report rejected rows, and expose one bulk operation. It is not a request to replace DDD, and support for
pluggable exposure strategies (TASK-127) and provider SPIs (TASK-180) is still planned work.

The current processor discovers `@Aggregate` types and builds `ClassManifest`s. `ApplicationWriter` generates the
aggregate service and delegates operation methods to plugins such as `CreatePlugin`; the plugins generate methods
in the context of that aggregate. `PrefabPlugin` is a useful extension contract, but its service/controller callbacks
are aggregate-scoped. Handwritten application services and repository mixins are already documented as the escape
hatch for composition in decision-005.

## Approaches compared

| Approach | Batch-import fit and ownership | Transaction boundary | Generated artefacts | Testability and customization | Migration cost |
|---|---|---|---|---|---|
| Aggregate-centric DDD (current) | Product owns product invariants; a batch import is an application workflow that coordinates repeated product creation. A custom import endpoint can be a handwritten adapter. | Each generated operation is transactional. A handwritten outer service can join them in one local transaction manager; this does not make broker events atomic. | Product repository, service, request/response and per-aggregate REST controller. No batch endpoint or row-level import result is generated. | Aggregate methods are unit-testable and generated services can be exercised through the existing test client. A handwritten batch adapter can be tested independently, but the custom HTTP contract is manual. Repository mixins remain available for bounded batch persistence. | None for existing projects. Keep using DDD and add a handwritten adapter/mixin only when needed. |
| Transaction script | The import operation owns the whole procedure and can be the clearest expression of row validation, partial rejection, and batching. It is not itself a packaging rule: it can be placed inside a vertical slice or an application service. | Explicitly chosen by the operation, usually one database transaction for all valid rows, or an explicitly designed chunk/partial-commit policy. | In a hand-written implementation, workflow, request/result, persistence port/adapter and endpoint are all manual. Generating only CRUD infrastructure adds little. | Straightforward to test with a fake persistence port; SQL batching, chunking, row-level errors, and external calls remain ordinary code. | Low as a handwritten service; high if Prefab invents a second CRUD-oriented runtime around it. |
| Vertical slice / use-case-owned operation | Best fit for the example: a named import feature owns its request, response, route and orchestration. The product aggregate/repository still owns product state and invariants; the slice does not become an alternative owner of the entity. | The feature explicitly owns the local transaction and calls persistence/domain collaborators. Cross-database or broker atomicity is not implied. | The spike generated a thin Spring MVC endpoint from one marked method. Workflow, DTOs and persistence port remained authored. A production generator would also need validation, error/status mapping, security, OpenAPI and client/test support. | Workflow is directly unit-testable using a fake port. Plain Java allows custom validation/batching; endpoint customization should be an extension, not business logic in a generated controller. | No migration of existing aggregates if strictly opt-in, but requires a new discovery/contract path and clear interop with existing endpoint generation. |
| Resource-centric application | A `Product` resource/controller/service owns operations, which works well for ordinary create/read/update/delete but not for one command spanning many products with a partial-rejection result. | Usually per-resource operation transaction; a bulk operation still needs an explicitly scoped coordinating transaction. | Similar to current aggregate generation: resource DTOs, service, repository and CRUD routes. A batch-specific route is either a custom method or a separate workflow. | Good for resource CRUD and generated clients. Bulk semantics and multi-resource rules tend to become custom hooks or controller logic, with less obvious ownership. | Low for CRUD if treated as a naming variation; substantial if bulk commands require new routing and transaction rules. |

These approaches are not mutually exclusive in general: a vertical slice can implement a transaction script and still
call aggregate-owned domain methods. The design decision here is about Prefab's generation unit and opt-in contract,
not about forbidding application services or transaction scripts in user code.

## Bounded prototype and comparison

The isolated source under `backlog/prototypes/task-309/` is deliberately not part of the Maven reactor. It annotates
`ProductImportWorkflow.execute` with a prototype-only `@UseCase(method = "POST", path = "/products/import")`.
`UseCaseProcessor` generates only a Spring MVC endpoint that validates the request body and delegates to that method.
The workflow, immutable request/result records, validation policy, transaction annotation, and persistence port are
hand-authored. The test compiles the generated endpoint with the JDK compiler, compares it with a checked-in output
snapshot, and separately tests the workflow using a fake port.

The product DDD reference is `examples/kafka` (`Product.java`, generated `ProductService`, `ProductController`, and
`CreateProductRequest`), compiled successfully for this spike. Its generated code is for single-product CRUD: the
create service constructs and saves one `Product`, and the controller exposes its resource operations. Reusing that
architecture for a batch requires a hand-written transaction-owning orchestrator and a batch HTTP adapter (or a
customized endpoint), invoking generated product operations or a repository mixin. DDD keeps the invariants and
generated CRUD contract clear; the vertical-slice prototype supplies the feature route from a marker but does not
remove the orchestration, request/result, validation, transaction, or persistence code. The comparison is therefore
about the missing feature-route plumbing, not fewer business lines or a like-for-like benchmark of generated CRUD.

Prototype scope limits: `ProductCatalog` has no production persistence adapter, so this is not a running Spring
application or a database/transaction integration test. The generated endpoint currently has no Prefab security,
tenant policy, OpenAPI metadata, error mapping, or generated test client. The prototype only supports one request
parameter and one marked operation per class. These omissions are intentional; the compile and unit tests establish
the smallest viable generation seam, not a production-ready public API.

## Decision

**No-go for adding a second built-in application architecture based on this evidence.** Keep aggregate-centric DDD
and its generated behavior as the default. Keep handwritten application services, repository mixins and custom
source overrides as opt-in escape hatches for workflows like batch import. Do not promote the prototype `@UseCase`
annotation or add a new runtime/module now.

The prototype proves a narrow adapter can be generated without changing DDD artifacts, but this alone is not enough
framework value: the operation body and persistence contract remain user-owned, and a production endpoint would
need to repeat or coordinate validation, authorization, tenant handling, error responses, OpenAPI and client
generation already supplied by the existing REST path. The current extension points cover custom logic, while
TASK-127 is already intended to separate exposure from business services. Adding a parallel annotation and endpoint
lifecycle now risks two competing contracts for the same HTTP surface.

Reconsider only when a concrete use case cannot be expressed safely with the existing aggregate plus adapter/mixin
pattern, and prototype it against the completed TASK-127 exposure contract. If evidence later supports adoption:

- Make it explicitly opt-in on a feature-owned operation; never infer or change architecture for existing
  `@Aggregate` types.
- Keep transaction orchestration and domain rules in developer-authored application code unless a separately
  specified contract demonstrates predictable generation value.
- Keep the generated boundary to exposure adapters and generated metadata/test support. Do not silently duplicate
  repositories, aggregate services, tenant rules, or CRUD operations.
- Define the feature transaction manager, authorization/tenant obligations, response/error mapping, and whether a
  command is single-database before promising generated behavior. Cross-service atomicity and outbox guarantees
  remain out of scope.

## Reusable contracts and isolation

- **Reusable:** `PrefabPlugin`/`PrefabContext` and `ServiceLoader` discovery; JavaPoet-based source generation;
  `FileOutput`/`OutputTargetFileOutput` for main-versus-test outputs; shared request parameter mapping, validation
  utilities, OpenAPI and generated-client patterns where the contracts actually match.
- **Not reusable without adaptation:** `ClassManifest`, aggregate service/controller callbacks, aggregate repository
  generation, and per-aggregate `@Generate` overrides assume an aggregate owner. They should not be reinterpreted
  as use-case metadata or made to generate feature routes implicitly.
- **Isolation if reconsidered:** use a separately named opt-in operation marker and writer/manifest; scope
  discovery to the current compilation; emit no files when the marker is absent; do not alter `@Aggregate`,
  existing plugin defaults or DDD generated output. Coordinate route ownership with TASK-127 so a route is generated
  once. Keep the prototype outside production source until those contracts are decided.

TASK-180 provider SPIs are complementary: they can decouple backend adapters but do not by themselves define
application workflow ownership or generated route semantics.

## Estimated implementation slices if adoption is justified later

These are rough sizing estimates, not approved follow-up tasks:

1. **S — Contract/design:** choose operation declaration and ownership; align route selection with TASK-127 and
   specify transaction, authorization, tenant, error, and output-target rules.
2. **M — Processor discovery:** create a use-case manifest/validation path that handles generated rounds and current
   compilation scope without relying on aggregate-only callbacks.
3. **M — Exposure generation:** generate route/request/response mapping and integrate validation, HTTP status/error
   behavior, security, OpenAPI, and collision checks.
4. **M — Developer support:** decide whether generated test clients/mothers apply, add compile/runtime tests, and
   document an end-to-end example with a persistence adapter.
5. **S — Compatibility/release:** prove a no-marker project produces byte-for-byte unchanged DDD output, document
   migration/opt-in behavior, and release only after TASK-127 integration.

The prototype covers only a small portion of slice 2 and a minimal portion of slice 3. It does not justify creating
these implementation tasks now.
