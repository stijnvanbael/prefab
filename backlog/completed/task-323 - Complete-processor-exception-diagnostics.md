---
id: TASK-323
title: Provide actionable compiler diagnostics for processor failure paths
status: Done
assignee: []
created_date: '2026-11-16 15:43'
labels: [dx, annotation-processor, bug]
dependencies: []
priority: high
---

## Description
<!-- SECTION:DESCRIPTION:BEGIN -->
Audit exception-producing paths in all Prefab processors and plugins so expected invalid inputs and generation failures yield actionable compiler diagnostics instead of uncaught processor stack traces. Preserve unexpected internal failures as explicit failures with diagnostic context, not broad swallowed exceptions or successful fallback output.

Related: completed TASK-007 (IDE errors), completed TASK-172 (validation messages) and TASK-307 (wildcard crash). Extend existing `ErrorReporter`; do not recreate the already-completed validation-message story.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Inventory explicit throws, parsing/conversion errors, file/resource failures and plugin boundaries across processor modules; classify expected input, infrastructure, programming and fatal JVM failures.
- [x] #2 Expected failures emit one useful ERROR diagnostic with source element/annotation where available, cause context and corrective action; suppress only consequent duplicate diagnostics, not independent errors.
- [x] #3 Unexpected programming/plugin failures fail compilation explicitly with processor/plugin identity and preserved cause for debugging; do not catch fatal JVM errors or silently continue emitting invalid artifacts.
- [x] #4 Add representative negative compilation tests per failure category, including unsupported types, invalid metadata, resources and generation I/O; no success-shaped fallback or raw unchecked escape remains for known invalid inputs.
- [x] #5 Document diagnostic conventions and plugin-author obligations, with a reproducible way to obtain detailed internal failure evidence.
<!-- AC:END -->

## Implementation Plan
<!-- SECTION:PLAN:BEGIN -->
Build a failure-path inventory, reuse the standard reporter at the closest context-bearing boundary, add negative fixtures and review remaining uncaught paths by classification.
<!-- SECTION:PLAN:END -->

## Implementation Notes
<!-- SECTION:NOTES:BEGIN -->
### Failure-path inventory and classification (AC #1)

| Area | Path | Class | Handling |
|------|------|-------|----------|
| Core | `TypeManifest` unsupported type (printed an error, then threw `IllegalArgumentException`) | expected input | `PrefabProcessingException`, reported once |
| Core | `TypeManifest.asClass` `ClassNotFoundException` | expected input / infrastructure | `PrefabProcessingException` with corrective action |
| Core | `TypeIdentity` `ClassNotFoundException`, `TypeManifest.asClassManifest` / `singleValueAccessor` state checks, `DatabaseChange`/`TableModification` state checks, `PluginOverride` null checks | programming | unchanged; reported as internal error naming the processor/plugin, cause preserved |
| Core | `JavaFileWriter`, `TestJavaFileWriter` I/O (the test writer printed to `System.err` and continued) | infrastructure | `PrefabProcessingException`; no success-shaped continuation |
| DB migration | `DataType.typeOf` "Unsupported type" (surfaced as a raw `IllegalArgumentException`) | expected input | `PrefabProcessingException` with `@CustomType` hint |
| DB migration | existing `db/migration` SQL that cannot be parsed/applied (`SqlStatementParser`, `Table`) | resource | `MigrationSqlException` -> `PrefabProcessingException` naming the file |
| DB migration | circular FK dependency, I/O reading/writing migrations | expected input / infrastructure | `PrefabProcessingException` |
| Mongo / AsyncAPI / Terraform | migration, `asyncapi.json`, terraform file I/O | infrastructure | `PrefabProcessingException` |
| Avro | unsupported field/logical/union types, unresolvable `@AvscInterface.type()`, malformed AVSC (was silently ignored or downgraded to a WARNING) | expected input / resource | `PrefabProcessingException` |
| Plugin boundary | every plugin hook (`initContext`, `writeEventFiles`, `writeGlobalFiles`, `writeAdditionalFiles`, `write*` per aggregate, dependency hooks, `dataTypeOf`) | programming / expected | `ProcessingGuard` names the plugin |
| Fatal JVM | `Error` subclasses | fatal | never caught |

Left as is, by design: `ClassNotFoundException` probes for optional modules, `MirroredTypeException` idiom, SHA-256
`NoSuchAlgorithmException`, `NumberFormatException`/`DateTimeParseException` fallbacks while *guessing* mother values
(they select a default, no artefact is invalid), and the aggregate-manifest write failure (WARNING, only used for
incremental detection).

### Design

* `PrefabProcessingException` (expected failure; element, tag, rule, fix, cause) and `ProcessingGuard`
  (the boundary) live in `processor.error`. `ErrorReporter.reportFailure` renders one `ERROR` per failure.
* Boundaries: whole round, every aggregate / polymorphic aggregate (manifest resolution and all writers), views and
  every plugin hook. Independent failures are all reported. A failed aggregate is excluded from later global files.
* `GeneratedFiles.buffered` holds back an aggregate's generated files until all layers succeeded, which removes the
  consequent `cannot find symbol` wave that a half-generated aggregate caused. `ErrorReporter` de-duplicates identical errors.
* Unexpected `RuntimeException`s: `[Prefab] Internal error in plugin <FQCN>` plus cause chain;
  `-Aprefab.diagnostics.stacktrace=true` adds the full stack trace.
* Docs: `backlog/docs/extension-points.md` section 8.8 and `CONTRIBUTING.md`.

### Tests

`ProcessorFailureDiagnosticsTest` (expected plugin failure per independent aggregate, unexpected plugin failure,
stack trace option, fatal error not caught, unsupported type, invalid existing migration),
`GenerationIoFailureTest` (generation I/O, guard de-duplication), `MalformedAvscDiagnosticsTest` (invalid resource).
A test-only `FailureInjectingTestPlugin` (inert unless `-Aprefab.test.failure=...`) is registered via
`META-INF/services` in the annotation-processor test resources.
Full suite: annotation-processor, avro-processor, kafka, pubsub, sns-sqs, terraform, core, streams, security all
green; the only failures in the reactor are Docker/Testcontainers integration tests that cannot run in this sandbox.
<!-- SECTION:NOTES:END -->
