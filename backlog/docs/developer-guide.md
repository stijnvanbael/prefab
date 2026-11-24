# Prefab Developer Guide

Prefab is an annotation-driven code-generation framework for Spring Boot applications built around the
**Aggregate Root** pattern. You write a plain Java record annotated with a handful of Prefab annotations;
the annotation processor generates a fully-wired Spring MVC controller, service, Spring Data repository,
request/response DTOs, event consumer, and database migration scripts at compile time.

> **Living document rule:** Any agent or developer that changes or adds a Prefab feature **must** update the
> relevant section of the appropriate guide below in the same commit/PR, together with the agent-facing material
> (cheat sheet, recipe, benchmark task) it affects. See
> [AGENTS.md, "Living document rule"](../../AGENTS.md#living-document-rule) for the full rule.
>
> Supported Java and Spring Boot versions are defined in the [readme](../../readme.md#quick-start); they are
> intentionally not repeated here so they cannot go stale.

---

## Core Concepts

| Concept              | Description                                                                                           |
|----------------------|-------------------------------------------------------------------------------------------------------|
| **Aggregate Root**   | A Java record annotated with `@Aggregate`. The single, consistent unit of data in the domain.         |
| **Event**            | A Java record (or sealed interface) annotated with `@Event`. Published to a messaging platform.       |
| **Event Handler**    | A method annotated with `@EventHandler`. Processes events to create or update aggregates.             |
| **Frontend View**    | A record annotated with `@ViewOf` that shapes aggregate data into a secured, paged HTTP contract.       |
| **Repository Mixin** | An interface annotated with `@RepositoryMixin`. Adds custom query methods to generated repositories.  |
| **Plugin**           | Implements `PrefabPlugin` and is registered via `META-INF/services`. Participates in code generation. |

---

## Documentation Index

| Document                                              | Contents                                                           |
|-------------------------------------------------------|--------------------------------------------------------------------|
| [Getting Started](getting-started.md)                 | Setup, first aggregate, quick-start example                        |
| [Module Dependency Matrix](modules.md)                | All modules, feature-to-module mapping, Maven snippets             |
| [Annotation Reference](annotation-reference.md)       | Every annotation with attributes, rules, and examples              |
| [Built-in Types](built-in-types.md)                   | `Reference<T>`, `Binary`, `AuditInfo`, `Page<T>`, provider interfaces, exceptions |
| [Generated Artefacts](generated-artefacts.md)         | Controller, service, repository, DTOs, consumers, migrations, test helpers, escape hatch, per-project agent context file, hand-written code checks |
| [Database Migration Lifecycle](database-migrations.md) | Schema diffing, generated/manual migrations, Flyway and MongoDB lifecycle               |
| [Feature Guides](feature-guides.md)                   | REST CRUD, aggregate-bound views, events, Avro, audit, multi-tenancy, SSE, testing (in-memory or containers) |
| [Extension Point Guide](extension-points.md)          | Customization coverage, `PrefabPlugin` API, `@RepositoryMixin`, provider overrides, escape hatch |
| [Configuration Reference](configuration.md)           | Application properties, Kafka/Pub/Sub/SNS configuration            |
| [Troubleshooting](troubleshooting.md)                 | Common errors and fixes                                            |
| [Agent Cheat Sheet](agent-cheat-sheet.md)             | Compact guide for AI agents building Prefab apps; copy into the project's `AGENTS.md` |
| [Task Recipes](recipes/index.md)                       | Small, verified recipes for common application intents; load only the matching recipe |

Generated records can be copied and modified with `toBuilder()`; see
[Generated Artefacts: Copying generated records](generated-artefacts.md#copying-generated-records)
for builder options and copy semantics.

For aggregate updates, prefer returning a new aggregate instance. The legacy `@Update void` form is
deprecated and scheduled for removal in the next major version; see the
[`@Update` reference](annotation-reference.md#update) for migration and async-commit guidance.
