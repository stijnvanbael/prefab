# Prefab Developer Guide

**Version:** 0.7.x | **Framework:** Spring Boot 4.x | **Java:** 21+

Prefab is an annotation-driven code-generation framework for Spring Boot applications built around the
**Aggregate Root** pattern. You write a plain Java record annotated with a handful of Prefab annotations;
the annotation processor generates a fully-wired Spring MVC controller, service, Spring Data repository,
request/response DTOs, event consumer, and database migration scripts at compile time.

> **Living document rule:** Any agent or developer that changes or adds a Prefab feature **must** update the
> relevant section of the appropriate guide below in the same commit/PR. See [AGENTS.md](../../AGENTS.md) for the full rule.

---

## Core Concepts

| Concept              | Description                                                                                           |
|----------------------|-------------------------------------------------------------------------------------------------------|
| **Aggregate Root**   | A Java record annotated with `@Aggregate`. The single, consistent unit of data in the domain.         |
| **Event**            | A Java record (or sealed interface) annotated with `@Event`. Published to a messaging platform.       |
| **Event Handler**    | A method annotated with `@EventHandler`. Processes events to create or update aggregates.             |
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
| [Generated Artefacts](generated-artefacts.md)         | Controller, service, repository, DTOs, consumers, migrations, test helpers |
| [Feature Guides](feature-guides.md)                   | REST CRUD, events, Avro, audit, multi-tenancy, SSE, testing        |
| [Extension Point Guide](extension-points.md)          | `PrefabPlugin` API, repository mixins, provider overrides          |
| [Configuration Reference](configuration.md)           | Application properties, Kafka/Pub/Sub/SNS configuration            |
| [Troubleshooting](troubleshooting.md)                 | Common errors and fixes                                            |


