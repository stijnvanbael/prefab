---
id: decision-009
title: Stable extension contracts versus internals
date: '2026-11-23'
status: accepted
related_tasks:
  - TASK-324
  - TASK-180
  - TASK-227
  - TASK-127
  - TASK-181
  - TASK-284
  - TASK-322
  - TASK-366
  - TASK-367
---

## Context

Prefab is opinionated: it generates controllers, services, repositories, migrations, messaging adapters, test
clients and documentation from annotations. Adopters need to adjust those opinions without forking Prefab or
taking over whole generated classes. Prefab already offers many ways to do that (TASK-324 inventory), but it never
said which of them are supported contracts, how they are discovered, in what order they run and what happens when
two contributions collide.

The spike tried thirteen realistic customizations against the current code (table below). Most are served by
declarative metadata or an existing hook. The genuine gaps are row-level read authorization, per-operation
observability and the reliability of `PrefabPlugin` itself: plugins ran in `ServiceLoader` discovery order, which
depends on the classpath order of plugin jars, and two plugins adding the same member produced an unattributed
javac error inside generated source.

## Customization scenarios

Classification: **declarative** (annotation, compiler option or property), **supported hook** (a documented
interface or bean), **whole-class override** (§6.11 escape hatch), **missing capability**, **protected invariant**
(deliberately not customizable).

| # | Scenario | Classification | How |
|---|---|---|---|
| 1 | Skip AsyncAPI documentation or a REST operation for one aggregate | Declarative | `@Generate(plugin = ..., enabled = false)`; project-wide `-Aprefab.plugin.<id>.enabled=false` |
| 2 | Query with a join or aggregate function | Supported hook | `@RepositoryMixin` with `@Query` |
| 3 | Screen-specific endpoint or composed command | Supported hook | Handwritten adapter over generated services (decision-005); `@ViewOf` for aggregate-bound views |
| 4 | Retry, back-off and dead-letter policy for one handler | Declarative + supported hook | `@EventHandlerConfig`, `prefab.dlt.*`; `CommonErrorHandler`, `ContainerCustomizer`, Kafka factory customizer beans |
| 5 | One public endpoint, CORS or an extra filter | Declarative + supported hook | `@Security(enabled = false)`; `HttpSecurityCustomizer` (TASK-284) |
| 6 | Topic serialization or registration not derived from `@Event` | Supported hook | `EventRegistryCustomizer` bean |
| 7 | Non-standard column type or value type | Declarative + supported hook | `@DbColumn`, `@CustomType` with `PrefabPlugin.dataTypeOf` / `avroSchemaOf` |
| 8 | Organisation plugin adding a method to every generated service next to Prefab's Kafka/Avro plugins | Supported hook, previously unreliable | `PrefabPlugin.writeService`; now with deterministic `order()` and attributed conflicts (prototype) |
| 9 | Data fix or column change that schema diffing cannot express | Supported hook | Manual `V{n}__*.sql` migration next to generated ones |
| 10 | Users only see their own orders on generated `getById` / `getList` | Missing capability | Today: override the service or write an adapter plus mixin. Already planned as TASK-322 |
| 11 | Latency and error metrics per generated operation and event consumer | Missing capability | Today: HTTP server metrics from Spring Boot and AOP on generated beans. TASK-366 |
| 12 | Change the generated controller of one aggregate beyond what annotations allow | Whole-class override | Copy to `src/main/java` (§6.11); Prefab stops generating it |
| 13 | Change id generation, drop `@Version` locking, edit an applied generated migration | Protected invariant | Identity, optimistic locking and migration history are correctness guarantees |

## Decision

### Stable contracts

These follow semantic versioning: breaking changes only in a major release, deprecation for at least one minor
release first.

- Annotations and enums in `be.appify.prefab.core.annotations` (including `rest`, `audit`, `validation`) and their
  documented attributes.
- Runtime provider interfaces: `TenantContextProvider`, `AuditContextProvider`, `HttpSecurityCustomizer`,
  `EventRegistryCustomizer`, event `Migration` beans, and beans documented as replaceable with
  `@ConditionalOnMissingBean`.
- Documented `prefab.*` application properties and `-Aprefab.*` compiler options.
- The externally visible shape of generated code: REST paths and payloads, public service and repository method
  names and signatures, generated type names and packages, event topics and schemas.
- The `PrefabPlugin` hooks, `order()` and `EXECUTION_ORDER`, the conflict rule below, `PrefabContext` event
  discovery methods and the read accessors of `ClassManifest`, `TypeManifest`, `VariableManifest` and
  `PolymorphicAggregateManifest`. These are **provisional** until TASK-367 marks the plugin API surface and adds
  binary-compatibility checks; until then they may change in a minor release with release notes. The plugin API
  exposes `com.palantir.javapoet` types, so the javapoet major version is part of this contract.

### Internals

Everything else, in particular: `*Writer` classes and other processor implementation types, generated method
bodies, private and package-private generated members, log messages, Spring configuration class and bean names
not documented as replaceable, and Prefab-internal modules' Java packages. Relying on them requires a whole-class
override, which the adopter then maintains (§6.11).

### Rules for every extension point

1. **Discovery.** Compile-time hooks are `PrefabPlugin`s found with `ServiceLoader`; runtime hooks are Spring
   beans collected with `ObjectProvider.orderedStream()`; declarative options are annotations, compiler options
   or properties. No classpath scanning or reflection on internals.
2. **No-op default.** With no contribution present, behaviour and generated output are unchanged. Every
   `PrefabPlugin` hook has an empty default; runtime hooks are optional beans.
3. **Deterministic order.** Plugins run by ascending `order()`, then fully-qualified class name. Runtime hooks
   follow Spring's `Ordered` / `@Order`. Output must not depend on classpath order.
4. **Defined conflicts.** Each hook states whether contributions compose (builder hooks, customizers), whether the
   first non-empty result wins (`requestBodyParameter`, `dataTypeOf`, ...) or whether a conflict is an error.
   A plugin that adds a method (same name and parameter types), field or nested type that Prefab or an earlier
   plugin already added to a generated controller, service, repository or test client fails compilation with an
   error on the aggregate naming both contributors.
5. **Regeneration safety.** Generated output is a pure function of sources and options: recompiling unchanged
   sources yields identical files, and handwritten code is never overwritten (§6.11 skip rule).
6. **Tests.** A new extension point ships with tests for its no-op default, its ordering and its conflict rule,
   and is documented in the [extension point guide](../docs/extension-points.md).

### Limits

- Prefab does not offer partial overrides of a generated method body. Use a hook around it (customizer, adapter,
  domain event) or take over the class.
- Plugins can only add to generated types; they cannot remove or rewrite members contributed by Prefab or by
  other plugins.
- After Prefab's attributed conflict error, javac still reports its own "already defined" error for the duplicate
  member, because javapoet builders do not allow removing a member.
- Conflict checks cover per-aggregate builder hooks. Whole files written by `writeAdditionalFiles`,
  `writeGlobalFiles` and `writeEventFiles` are owned by the plugin that writes them; two plugins writing the same
  file fail through the `Filer`.

## Prototype evidence

`PrefabPlugin.order()` / `EXECUTION_ORDER` and `PluginContributions` (annotation processor) were implemented and
tested in `PluginContributionsTest`: plugins listed in reverse discovery order produce identical output; equal
orders tie-break on class name; adding a plugin that overrides nothing leaves every generated file byte-identical;
two compilations of the same sources are identical; duplicate methods, generator-owned fields and the generated parent
lookup of a child repository are reported with both contributors; overloads do not conflict. The repository writer
now adds the parent lookup before the plugins run so that it takes part in the check. All 527 annotation-processor tests stayed green, so moving the
built-in plugins from service-file order to class-name order did not change any expected output.

## Consequences

- Third-party plugins can rely on a documented position relative to Prefab's plugins and get actionable errors.
- Generated member order inside a type may differ from earlier releases where several plugins contribute to the
  same type. This is cosmetic.
- TASK-227's per-plugin toggles are already implemented through `-Aprefab.plugin.<id>.enabled` and `@Generate`;
  only its "default values" part remains open.
- Follow-ups: TASK-366 (Micrometer observations) and TASK-367 (mark and check the plugin API surface). Row-level
  read authorization is already planned as TASK-322. TASK-180 (persistence/messaging provider SPIs) and TASK-127
  (exposure modules) remain the vehicles for backend and exposure extensibility; this decision adds no competing
  SPI.
