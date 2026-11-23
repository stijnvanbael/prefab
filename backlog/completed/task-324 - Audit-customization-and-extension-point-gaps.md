---
id: TASK-324
title: Spike customization coverage and identify missing extension points
status: Done
assignee: []
created_date: '2026-11-16 15:43'
labels: [spike, architecture, dx]
dependencies: []
priority: medium
---

## Description
<!-- SECTION:DESCRIPTION:BEGIN -->
Determine which Prefab opinions can be customized through supported declarative metadata, generation plugins, repository mixins and runtime providers, and identify concrete gaps that currently require forks or replacing whole generated classes. Aim for useful safe customization, not an unbounded promise that every internal detail is public API.

Related: TASK-180 (provider SPIs), TASK-227 (plugin configuration), TASK-127 (exposure modules), completed TASK-181 (escape-hatch docs) and completed TASK-284 (security hook). Reuse their contracts rather than proposing duplicates.
<!-- SECTION:DESCRIPTION:END -->

## Analysis
<!-- SECTION:ANALYSIS:BEGIN -->
Inventory of the current customization surface (2026-11-23):

- **Compile time.** `PrefabPlugin` (ServiceLoader) exposes per-aggregate builder hooks (`writeController`, `writeService`, `writeRepository`, `writeTestClient`, polymorphic variants), service dependencies, request-parameter mapping, `@CustomType` column/Avro mapping and whole-file hooks (`writeAdditionalFiles`, `writeGlobalFiles`, `writeEventFiles`). Plugins are toggled project-wide with `-Aprefab.plugin.<id>.enabled` and per aggregate with `@Generate(plugin, enabled, target)`; that part of TASK-227 is already implemented although the task is still `To Do`.
- **Declarative metadata.** `@Aggregate(exposure, plural)`, operation `path`/`method`/`exposure`/`security`, `@Filter`, `@DbColumn`, `@DbRename`, `@DbDefaultValue`, `@Indexed`, `@DbMigration(enabled)`, `@Event(topic, platform, serialization, publishTo, version, compatibility)`, `@EventHandlerConfig` (concurrency, DLT, retries), `@Doc`, `@ViewOf`.
- **Runtime.** `TenantContextProvider`, `AuditContextProvider`, `HttpSecurityCustomizer`, `EventRegistryCustomizer`, Spring Kafka `DefaultKafka{Producer,Consumer}FactoryCustomizer`, `ContainerCustomizer`, `CommonErrorHandler`, event `Migration` beans, `@ConditionalOnMissingBean` replacements of executors/serializers/registries, `prefab.dlt.*` and `prefab.streams.*` properties.
- **Escape hatches.** `@RepositoryMixin`, handwritten adapters over generated services (decision-005), manual Flyway migrations next to generated ones, and whole-class source override (§6.11).

Gaps observed in the code:

1. Plugin execution order is the `ServiceLoader` discovery order, which depends on classpath order across jars (`kafka`, `pubsub`, `sns-sqs`, `avro-processor`, `terraform` and third-party plugins). Plugins cannot declare ordering and two plugins adding the same member produce a javac error in generated code with no attribution. This makes third-party `PrefabPlugin`s fragile and is the highest-value gap because every other extension story builds on it.
2. Row-level (non-tenant) read authorization on generated `getById`/`getList` is only possible by overriding the service or writing an adapter.
3. Generated services, repositories and event consumers emit only `log.debug`; there is no opt-in Micrometer observation.
   Row-level read authorization (gap 2) is already planned as TASK-322.
4. `extension-points.md` §8.5 documents a non-existent `SerializationRegistryCustomizer`; the real contract is `EventRegistryCustomizer`.
<!-- SECTION:ANALYSIS:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Produce a coverage matrix spanning generated contracts/controllers/services/repositories, persistence/migrations, messaging/serialization, security, tests, observability and infrastructure, linked to actual extension APIs.
- [x] #2 Identify at least five realistic customization scenarios and classify each as declarative, supported hook, whole-class override, missing capability or intentionally protected invariant.
- [x] #3 Prototype the highest-value missing hook and verify deterministic ordering, no-op default behavior, conflict handling and regeneration safety.
- [x] #4 Record an ADR distinguishing stable extension contracts from internals, with compatibility/discovery/testing requirements and explicit limits.
- [x] #5 Create prioritized, bounded implementation stories for genuine gaps after duplicate checks, and update the extension guide with current capabilities and supported escape hatches.
<!-- AC:END -->

## Implementation Plan
<!-- SECTION:PLAN:BEGIN -->
Inventory current hooks/configuration, test representative advanced customizations, select one gap for a bounded prototype and propose only evidence-backed API additions.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- **AC #1 coverage matrix:** new §8.0 "Customization Coverage" in `backlog/docs/extension-points.md`, one row per area (plugin selection, REST, services, repositories, persistence/migrations, messaging/serialization, security, tenancy/audit, tests, observability, infrastructure), each linked to the actual annotation, hook or bean.
- **AC #2 scenarios:** thirteen scenarios classified in [decision-009](../decisions/decision-009%20-%20Stable-extension-contracts-versus-internals.md) (declarative, supported hook, whole-class override, missing capability, protected invariant).
- **AC #3 prototype:** the highest-value gap was `PrefabPlugin` reliability, which every other compile-time extension builds on.
  - `PrefabPlugin.order()` (default `DEFAULT_ORDER = 0`) and `EXECUTION_ORDER` (order, then class name). `PrefabProcessor` sorts discovered plugins, so output no longer depends on classpath order.
  - `PluginContributions` replaces the four copies of `applyPlugins` in `HttpWriter`, `ApplicationWriter`, `PersistenceWriter` and `TestClientWriter`. It snapshots the builder after each plugin and reports a compiler error on the aggregate naming both contributors when a plugin adds a method (name and parameter types), field or nested type that Prefab or an earlier plugin already added. javapoet builders cannot remove members, so javac's own duplicate error still follows.
  - `PersistenceWriter` now adds the child-repository parent lookup before the plugins run, so it is included in the conflict check.
  - A package-private `PrefabProcessor(List<PrefabPlugin>)` constructor lets tests inject plugins.
  - `PluginContributionsTest` (8 tests) covers ordering independent of discovery order, the class-name tie-break, a no-op plugin leaving every generated file identical, identical regeneration, plugin–plugin, plugin–generator and parent-lookup conflicts, and overloads not conflicting.
  - Built-in plugins now run in class-name order instead of service-file order. No expected output in the annotation-processor suite changed.
- **AC #4 ADR:** decision-009 lists stable contracts versus internals, the discovery, no-op, ordering, conflict, regeneration and testing rules, and explicit limits. The plugin API is marked provisional until TASK-358.
- **AC #5 stories and docs:** duplicate checks found TASK-322 for row-level authorization (referenced, not duplicated) and found TASK-227's toggles already implemented (note added to TASK-227). New stories: TASK-357 (opt-in Micrometer observations, medium) and TASK-358 (mark the plugin API and check binary compatibility, low). The extension guide gained §8.0 and "Plugin order and member conflicts", and the stale §8.5 `SerializationRegistryCustomizer` was replaced by the real `EventRegistryCustomizer`. CONTRIBUTING.md and the developer guide index were updated, and `built-in-types.md` was fixed (`EventType.of(...)` in the `register` example, removed the section on the deleted `SerializationRegistry`).
- **Verification:** `mvn -pl annotation-processor,kafka,pubsub,sns-sqs,avro-processor,terraform install` passed, including the full annotation-processor suite. Example modules were not run because they need Testcontainers.
<!-- SECTION:NOTES:END -->
