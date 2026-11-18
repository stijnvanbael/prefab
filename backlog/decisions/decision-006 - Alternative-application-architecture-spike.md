---
id: decision-006
title: Screen alternative application architectures before deciding adoption
date: '2026-11-18'
status: proposed (revised after broad use-case screen)
related_tasks:
  - TASK-309
  - TASK-127
  - TASK-180
  - TASK-319
  - TASK-326
  - TASK-327
  - TASK-328
  - TASK-329
  - TASK-330
  - TASK-331
  - TASK-332
  - TASK-333
  - TASK-334
  - TASK-335
---

## Context

TASK-309 asked whether Prefab should generate an additional application architecture for workflows that do not map
cleanly to one aggregate. The first assessment used one representative case: importing a batch of products. That
was too little evidence to support a framework-wide no-go. This revision screens fourteen materially different use
cases to distinguish aggregate-model mismatch from capabilities already present in Prefab, planned work, or
specialized infrastructure. The objective remains to evaluate an optional generation model, not to replace DDD.

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

## Broader use-case screen

This is a qualitative screen, not proof that each area needs a Prefab feature. “DDD fit” asks whether an aggregate is
a natural primary unit for the work. “Prefab coverage” distinguishes built-in generation from manual composition
and known roadmap items. “Screen result” identifies what deserves deeper evidence, not an adoption commitment.

| # | Use case | DDD fit | Relevant Prefab coverage today | Screen result |
|---|---|---|---|---|
| 1 | Reference-data administration (countries, tax codes, units) | Good: stable identity, validation and lifecycle map to simple aggregates. | Generated CRUD, filters, pagination, optimistic locking and persistence backends directly cover this shape. | **Not a mismatch.** Useful baseline/control case; do not add a competing resource model without evidence CRUD is insufficient. |
| 2 | Large catalogue import with row-level rejection, checkpoints and resume | Weak at the workflow boundary: the unit of work is a job/chunk, while product invariants still belong to products. | Generated create is per aggregate. A handwritten application service and repository mixin can implement it; no built-in batch job/checkpoint contract is shown. | **Strong candidate.** Prototype bulk persistence vs per-item aggregate operations, partial failure, transaction/chunk semantics and restartability. |
| 3 | Operational report joining orders, payments and delivery totals | Weak: output is a query projection rather than an aggregate lifecycle. | `@GetList`, filters and repository mixins exist; cross-aggregate joins/projections are explicitly in TASK-319. | **Strong candidate, but not a new write architecture.** Compare generated projection support with current mixin/manual adapter and TASK-319. |
| 4 | Frontend dashboard combining several domain records | Weak-to-moderate: screen response boundaries do not necessarily match aggregate boundaries. | Handwritten adapters are documented and demonstrated; declarative views/reference expansion are planned in TASK-326/327. | **Already identified.** Use as a cross-aggregate read case in projection evaluation; avoid duplicating the view roadmap. |
| 5 | Faceted product search with relevance, highlighting and cursors | Weak for the search result contract; aggregate lifecycle remains useful for writes. | Generated full-text/equality/range filters and paged lists cover simpler searches. External search-index facets, score and cursor semantics are not established by that contract. | **Partial gap.** Investigate as a specialized query adapter, not evidence for a wholesale application architecture. |
| 6 | Payment-provider webhook ingestion with signature check, deduplication and routing | Weak at ingress: external delivery/authentication protocol precedes aggregate mutation. | Prefab generates broker event consumers/handlers, but that is not evidence of an HTTP webhook signature, replay or delivery contract. | **Potential integration gap.** Assess only if provider-specific webhook adapters recur; otherwise retain as manual adapter/provider concern. |
| 7 | Stateless shipping-rate or eligibility façade over remote providers | Poor: may have no local entity, repository or aggregate lifecycle. | Current built-in REST generation is aggregate-operation-centric; plugin hooks have aggregate manifests. No general operation-only exposure contract is evident; TASK-127 concerns exposure strategies, not necessarily aggregate-free operations. | **Strong candidate.** Test whether typed request/response plus generated exposure is reusable without inventing persistence or domain state. |
| 8 | Scheduled bank-settlement reconciliation with discrepancy output | Weak at job boundary: work is dataset comparison, checkpoint and discrepancy reporting; later resolution may be aggregate behavior. | Event handlers and aggregate CRUD can support downstream mutations, but a scheduled/checkpointed reconciliation lifecycle is not established by the current generated contracts. | **Potential batch family.** Compare with catalogue import; avoid a second prototype unless resumability/consistency semantics differ materially. |
| 9 | Asynchronous document conversion with progress and downloadable result | Mixed: a job itself has identity/status and can be an aggregate; conversion execution is a worker pipeline. | Binary fields and `@Download` support file downloads; event handlers and async commit support asynchronous processing patterns. A generated job lifecycle/worker contract is not established. | **Partial mismatch, not a clean anti-DDD case.** Check whether existing aggregate + events + binary support suffices before proposing job scaffolding. |
| 10 | Telemetry ingestion, windowed aggregation and threshold detection | Poor for stream execution: ownership follows keys, windows and topology state. | The optional `streams` module already provides a Kafka Streams DSL, source/sink, aggregation and state-store abstractions. | **Already a distinct architecture surface.** Evaluate completeness of the streams module separately; do not use it to justify another generic application architecture. |
| 11 | Multi-system onboarding with retries and compensation | Weak for workflow coordination: no single local transaction spans identity, CRM and billing. Individual state may still be aggregate-owned. | Events, messaging modules, handlers and `@AsyncCommit` support message-driven integration; these do not establish durable saga state, compensation or outbox atomicity. | **Important but high-risk gap.** Requires a separate durable-workflow assessment; do not promise it as a side effect of transaction-script generation. |
| 12 | Stateless tax/eligibility calculation from a request and versioned rules | Poor: computation may be a pure operation with no persistent identity. | Aggregate operation generation assumes an aggregate; no generic request/response operation generation is evident in the screened contracts. | **Strong candidate.** Prototype operation-only generation; quantify how much endpoint/client/validation plumbing is truly reusable. |
| 13 | Administration over a legacy shared schema/stored procedures | Weak when the schema boundary is externally owned, though domain concepts may still exist. | PostgreSQL/MongoDB providers target generated aggregate repositories. TASK-180 proposes provider SPIs; it does not by itself specify arbitrary legacy mapping or procedure contracts. | **Provider/data-mapping concern, not yet architecture evidence.** Screen provider SPI scope before adding another application model. |
| 14 | Large authorized CSV export with streaming or asynchronous download | Weak at the export job/query boundary, but source records can remain aggregate-owned. | Paged `@GetList` and binary download are present; a long-running, checkpointed export artifact lifecycle is not established. | **Potential query/job gap.** Assess alongside reporting and batch import; keep tenant/row authorization and resource limits explicit. |

### Screen method and prioritization

The screen considers: (a) whether aggregate identity/invariants are central to the use case, (b) whether Prefab
already generates the relevant contract, (c) whether existing extension points can meet it without unsafe
duplication, and (d) whether repeated plumbing gives Prefab a bounded generation opportunity. A poor DDD fit alone
is insufficient: search, webhooks and legacy persistence may need specialized adapters rather than another
application architecture.

The most informative next comparative prototypes are:

1. **Stateless operation** (cases 7/12): no aggregate/repository, typed input/output, validation, REST exposure and
   generated client. This tests whether generation can be useful without resource lifecycle assumptions.
2. **Cross-aggregate projection** (cases 3/4/5): paged, filtered query with stable ordering, counts and authorization.
   Compare current mixins/manual composition against TASK-319/326/327; do not duplicate that planned scope.
3. **Resumable bulk job** (cases 2/8/14): batch persistence/export with an explicit all-or-nothing vs chunk commit
   policy, checkpoints, retries and result reporting. Compare against both per-aggregate DDD and the existing
   batch-import prototype.
4. **Durable external workflow** (cases 6/11): include only as a boundary study to establish whether Prefab needs
   integration ingress or durable orchestration, not as a presumed member of the same architecture family.

Cases 1 and 10 serve as controls: existing aggregate CRUD is a good fit for simple reference resources, and Prefab
already has a separate streams DSL. Cases 5, 6, 9, 13 and 14 need focused evidence, but should not automatically
be folded into a generic alternative. The current spike prototype only covers the route seam of case 2; it does not
test batch persistence, checkpoints, or database transaction behavior.

### Deferred backlog follow-ups

The distinct potential gaps are tracked as low-priority assessments, not approved implementation commitments:
TASK-329 (aggregate-free operations), TASK-330 (resumable import/export jobs), TASK-331 (faceted search),
TASK-332 (incoming webhooks), TASK-333 (durable cross-service workflows), TASK-334 (legacy schemas), and
TASK-335 (asynchronous file processing). TASK-319 and TASK-326/327 already cover cross-aggregate projections
and views. TASK-105, TASK-107, TASK-157, and TASK-317 cover related synchronous bulk operations, exports,
job status, and database backfills; the new assessments must compare and reuse those contracts.

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

**Decision deferred: this screen invalidates the prior broad no-go, but does not yet justify adoption.** Four
candidate families (operation-only services, query projections, resumable jobs, durable external workflows) have
plausible structural gaps, while the screen also found important cases already served by aggregates, existing
modules, or planned view/projection work. They do not form one obvious alternative architecture.

Keep aggregate-centric DDD as the default and preserve existing behavior. Keep the prototype `@UseCase` annotation
non-public and isolated. Do not add a production runtime/module until comparative prototypes show a coherent,
repeated generation contract with more value than Prefab's current extension points and planned TASK-127/319/326
work.

If later prototypes support adoption:

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

The current prototype covers only a small portion of slices 2 and 3. The four comparative proof points above
should be completed before deciding whether to create implementation tasks for a new architecture or to close the
identified gaps through TASK-127, TASK-319/326, existing provider work, or focused integration features.
