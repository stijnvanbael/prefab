---
id: TASK-395
title: Preserve Avro schema-only native IntelliJ incremental builds
status: To Do
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
  - TASK-394
  - backlog/docs/troubleshooting.md
priority: high
type: bug
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Complete native IntelliJ/JPS incremental support for `.avsc`-only changes to an `@Avsc` contract. This is the
unresolved scenario #6 split from TASK-310 so the verified Java-model incremental improvements can ship separately.
Do not require Maven delegation, invalidating IDE caches, or changes to compiler settings.

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
JPS copies a changed `.avsc` without recompiling the owning `@Avsc` interface, so resource-only invalidation
must be considered separately from Java source ownership. After explicit contract recompilation, JPS can delete
owned CLASS_OUTPUT resources before javac runs; investigate how the migration writer reconstructs the full
aggregate history and where V1 and the manifest are lost. Preserve the observed failing output in the TASK-310
session's `files/jps-avro-failed` snapshot if available; reproduce independently rather than depending on that
session. Do not mask an incomplete migration with a clean build or by disabling incremental compilation.
<!-- SECTION:ANALYSIS:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Capture the JPS build modes, source inputs, Filer/resource ownership, and migration history before and after the schema-only edit; identify why the contract is not recompiled and why V1/the manifest disappear.
- [ ] #2 Provide a repeatable native workflow for `.avsc` changes that regenerates the record and dependent test accessor without stale generated Java or invalid migrations, without Maven delegation or compiler-setting changes; if JPS cannot discover resource-only changes automatically, document an explicit scoped recompile and verify it end-to-end.
- [ ] #3 After editing the schema and dependent test, generated Java, test helpers, migrations (including all aggregate tables), parsed AsyncAPI, event baselines, and main/test class paths match a clean Maven build of the same edits; no obsolete event inputs or generated files remain after reverting.
- [ ] #4 Add automated regressions for consecutive partial compilations that expose the schema/migration failure and prove complete migration history across source and resource changes.
- [ ] #5 Update the IDE troubleshooting guidance with only the verified workflow and limitations. Keep the invalid-replay diagnostic improvement in TASK-394.
<!-- AC:END -->
