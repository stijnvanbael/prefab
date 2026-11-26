---
id: TASK-395
title: Preserve native incremental builds after Avro schema changes
status: Done
assignee: []
created_date: '2026-11-26 11:39'
labels:
  - bug
  - dx
  - annotation-processor
  - avro
  - migrations
dependencies: []
references:
  - TASK-310
  - TASK-388
  - TASK-394
  - backlog/docs/troubleshooting.md
priority: high
type: bug
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Complete native IDE incremental support for `.avsc`-only changes to an `@Avsc` contract. This is the unresolved
scenario #6 split from TASK-310 so the verified Java-model incremental improvements can ship separately. Do not
require Maven delegation, invalidating IDE caches, or changes to compiler settings. Prefer a workflow that applies
across IDEs instead of relying on IntelliJ-specific integration.

In IDEA 2026.3 (IU-263.7307.200), JPS javac 25.0.1, rename `trackingNumber` to `trackingCode` in
`examples/showcase/src/avro/resources/avro/shipment-dispatched.avsc`. A native Build Project copies the changed
resource but does not invoke javac for `ShipmentEvent.java`, leaving `ShipmentDispatched` stale. Update the
dependent `ShipmentIntegrationTest` accessor, recompile `ShipmentEvent.java`, then Build Project: a generated
`V2__generated.sql` contains only an `ALTER TABLE customer_order_history_entry ... ADD CONSTRAINT` while
`V1__generated.sql` and `prefab-aggregates.txt` are absent from `target/classes/db/migration`. Migration replay
fails because the earlier table definition is missing. A separate clean Maven build of the same edits succeeds
with the V1 migration and manifest. Native generated Java and helpers, parsed AsyncAPI, and event baselines
matched the clean reference before the failed Make, but only 6 of 219 main class paths remained afterward.
The misleading generic SQL suggestion is tracked separately in TASK-394; do not fix it by editing generated SQL.
<!-- SECTION:DESCRIPTION:END -->

## Analysis

<!-- SECTION:ANALYSIS:BEGIN -->
Use a portable workflow: after changing an `.avsc`, explicitly compile its owning `@Avsc` Java source, then run the
IDE's normal incremental build. JSR 269 has no portable way for an annotation processor to register arbitrary
resource files as compiler inputs, so automatic invalidation would require IDE/build-system-specific integration or
changing the schema authoring contract; neither is in scope.

Native JPS reproduction on IDEA 2026.3 / javac 25.0.1:

- A full JPS rebuild started with V1 and `prefab-aggregates.txt`. V1 creates
  `customer_order_history_entry` with an inline `REFERENCES` clause.
- A selected compile of `ShipmentEvent.java` regenerated `ShipmentDispatched` from the renamed field, but also wrote
  V2 containing only an `ADD CONSTRAINT` for that same inline foreign key. This matches the independent parser bug in
  TASK-388; the Avro rename itself does not change that aggregate.
- The following native Make ran as `isMake:true`, compiled 51 production Java sources, then failed while replaying
  V2. JPS had removed V1 and `prefab-aggregates.txt` from `target/classes/db/migration`; only the orphan V2 remained.
  The processor could not replay that file without V1, so it did not recreate the manifest.

The user approved including TASK-388's inline-foreign-key parser fix because it blocks this exact end-to-end
workflow. Keep the two defects distinct in tests: verify inline foreign-key replay independently, then cover the
Avro resource edit, scoped contract compile, and following aggregate recompilation as consecutive partial
compilations. Preserve the TASK-310 snapshot as corroboration, but continue to base implementation tests on
reproducible builds.

Do not mask incomplete output with a clean build, disabling incremental compilation, or changing generated SQL.
<!-- SECTION:ANALYSIS:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Capture the JPS build modes, source inputs, Filer/resource ownership, and migration history before and after the schema-only edit; identify why the contract is not recompiled and why V1/the manifest disappear.
- [x] #2 Provide a repeatable native workflow for `.avsc` changes that regenerates the record and dependent test accessor without stale generated Java or invalid migrations, without Maven delegation or compiler-setting changes; if JPS cannot discover resource-only changes automatically, document an explicit scoped recompile and verify it end-to-end.
- [x] #3 After editing the schema and dependent test, generated Java, test helpers, migrations (including all aggregate tables), parsed AsyncAPI, event baselines, and main/test class paths match a clean Maven build of the same edits; no obsolete event inputs or generated files remain after reverting.
- [x] #4 Add automated regressions for consecutive partial compilations that expose the schema/migration failure and prove complete migration history across source and resource changes.
- [x] #5 Update the IDE troubleshooting guidance with only the verified workflow and limitations. Keep the invalid-replay diagnostic improvement in TASK-394.
<!-- AC:END -->

## Implementation Notes

- `@Avsc` reads its `.avsc` through `CompilerResources.open` from the class path/output. The resource is not a javac source input, so JPS's normal Build Project may copy the changed resource without invoking the contract's processor. The portable workaround is to compile the owning `@Avsc` Java source explicitly, then run the ordinary incremental build.
- The original native JPS failure also exposed TASK-388: JSqlParser returned the inline `REFERENCES` clause as one `columnSpecs` entry, which the migration parser did not record. The selected contract compile therefore generated a redundant V2 constraint; the following Make removed V1 and the aggregate manifest before failing to replay that orphaned V2. The parser fix now records the inline reference, so no spurious V2 is generated and the following Make recreates the complete V1 and manifest.
- Verified with IDEA 2026.3 / JPS javac 25.0.1: rebuilt the original showcase, changed the schema field from `trackingNumber` to `trackingCode` and updated its test accessor, compiled only `ShipmentEvent.java`, then ran normal Build Project. JPS succeeded. The native output matched the saved clean reference exactly: 130 generated main sources, 116 generated test helpers, 2 migration files, 1 parsed AsyncAPI file, 16 event baselines, 219 main class paths, and 218 test class paths, with no missing, extra, or differing files.
- Reverted the schema and test edits, recompiled `ShipmentEvent.java`, and ran Build Project again. The generated event returned to `trackingNumber`; it contained no `trackingCode`, V1 and its manifest existed, and no V2 remained.
- Added `AvscIncrementalCompilationTest` for the resource edit → contract-only compile → following all-source compile sequence after JPS-like cleanup of the shared migration/manifest. It verifies regenerated Avro Java and complete migration history. Added SQL parser and processor regressions for inline foreign-key replay and a later model delta. The native `SqlStatementParserTest`, full `DbMigrationWriterTest`, changed-model regression, Avsc incremental regression, and full `AvscPluginTest` passed.
- Updated the IDE troubleshooting guidance, README, migration lifecycle guide, and agent cheat sheet. No annotation/API contract or generated agent-context shape changed; the fix corrects migration-history replay and the IDE workflow remains an explicit scoped compile.
