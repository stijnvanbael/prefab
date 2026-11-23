---
id: TASK-347
title: Detect hand-written code that duplicates what Prefab generates
status: Done
assignee: []
created_date: '2026-11-20 20:14'
labels:
  - agents
  - dx
  - annotation-processor
dependencies: []
priority: medium
type: feature
ordinal: 192000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The most common way AI models (and new users) misuse Prefab is to ignore it: they write a @RestController, a JPA @Entity, a Spring Data repository or a migration script by hand for an aggregate that Prefab already covers. The result often compiles and may even pass simple tests, so nothing tells the model it went wrong. The app ends up with two competing implementations, conflicting endpoints or drifting schemas.

Documentation alone (TASK-344) reduces this but cannot rule it out. A fast, automatic check that flags these patterns gives agents a clear signal to correct course, and benefits human developers too.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Hand-written controllers that map paths already exposed for a Prefab aggregate are reported, naming the aggregate and the clashing endpoint
- [x] #2 JPA entities or hand-written repositories for Prefab aggregates are reported
- [x] #3 Hand-written database migrations that alter tables Prefab manages are reported, unless they use a documented escape hatch
- [x] #4 Each finding explains what Prefab already provides and the recommended alternative or extension point, in the same 'Suggested fix' style as existing processor diagnostics
- [x] #5 Legitimate customisation through documented extension points (repository mixins, plugins, provider overrides, the source-file escape hatch) is not reported
- [x] #6 The checks run as part of a normal build or test run, can be turned off per project, and are covered by positive and negative tests
- [x] #7 The checks are documented in the developer guide and mentioned in the agent cheat sheet
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- New `HandWrittenCodePlugin` (`annotation-processor/.../processor/handwritten`). It remembers the root elements of the *first* processing round (the project's own sources; later rounds only hold generated sources) and, from `writeGlobalFiles`, compares them with the resolved aggregates:
  - `HandWrittenControllerCheck`: `@RestController`/`@Controller` handlers whose method+path equals an endpoint Prefab exposes (AC #1). The endpoint list comes from the new `rest/AggregateEndpoints`, extracted from `AgentContextRenderer` so the agent context and the check describe the same surface. Path variable names are ignored; a mapping without HTTP method matches every method. Polymorphic aggregates are not compared (no endpoint list for them).
  - `HandWrittenPersistenceCheck`: `@Entity` (Jakarta/javax), `@Table` and `@Document` types named like an aggregate or mapped to its table/collection, and types extending Spring Data `Repository<T, ID>` with an aggregate as `T` (AC #2). Annotations are matched by qualified name through annotation mirrors, so JPA does not have to be on the processor path.
  - `ManualMigrationCheck` (package `dbmigration`, called from `DbMigrationPlugin` right after the migration is written, because the managed table names - including child tables - come from `DbMigrationWriter.desiredDatabaseState`, now returned by `writeDbMigration`): versioned migrations in `db/migration` that CREATE/ALTER/DROP a managed table or CREATE INDEX on it (AC #3). The directory lookup moved from `DbMigrationWriter` into the reusable `MigrationDirectory`.
- Escape hatches (AC #5): `@RepositoryMixin` interfaces and plugins are never matched; a controller/repository with the generated simple name in the aggregate's package tree (source-file override, §6.11) is skipped; migrations named `*generated`, `*manual` or `*baseline`, or containing `-- prefab:manual`, are skipped; `@SuppressWarnings("prefab.handwritten")` on a class or handler method silences a legacy implementation.
- Messages use the existing `[Prefab] <element>: <rule>\nSuggested fix: ...` format (AC #4) via `ErrorReporter`, which gained `reportValidationWarning`.
- Severity: Java findings are errors (clear signal for agents), migration findings are warnings by default because a renamed copy of a generated script (the documented flow, and what several existing `DbMigrationWriterTest` cases do) cannot be told apart from a hand-written one. `-Aprefab.handwritten.severity=error|warning` overrides both. See decision-010.
- Opt-out (AC #6): `-Aprefab.plugin.handwrittencode.enabled=false` (standard plugin switch). Covered by `HandWrittenCodePluginTest` (positive and negative cases for controllers, entities, repositories, migrations, severity and the switch). The migration negative case includes one flagged file so it cannot pass vacuously.
- Docs (AC #7): `generated-artefacts.md` §6.13, `database-migrations.md`, `troubleshooting.md`, `extension-points.md` §8.6, developer-guide index. The agent cheat sheet does not exist yet, so TASK-344 carries a note to reference the checks.
- Known limits: sources of other modules and code generated by other processors are not inspected; the processor stops before this check when an existing migration cannot be parsed (that error is reported instead).
<!-- SECTION:NOTES:END -->
