---
id: TASK-310
title: Make IntelliJ IDEA incremental compilation reliable without compiler-setting workarounds
status: In Progress
assignee: []
created_date: '2026-11-16 15:43'
labels: [bug, dx, annotation-processor]
dependencies: []
priority: high
---

## Description
<!-- SECTION:DESCRIPTION:BEGIN -->
Investigate and fix Prefab generation failures or stale output during IntelliJ IDEA incremental compilation using its normal annotation-processing support. Resolve processor lifecycle, dependency tracking, metadata and generated-file ownership problems rather than requiring Maven delegation, cache invalidation or compiler-setting changes.

Related: completed TASK-168 (compilation performance) and TASK-307 (multi-round generated records). The README currently recommends compiler delegation; only remove this guidance when the supported native workflow is demonstrated.
<!-- SECTION:DESCRIPTION:END -->

## Analysis

<!-- SECTION:ANALYSIS:BEGIN -->
- Baseline: repository targets Java 21; IntelliJ project annotation processing is enabled for the six example modules. The local Maven launcher is Maven 3.9.16 on JDK 25.0.1, while `java -version` resolves to JDK 21.0.8. The local IntelliJ installation path identifies version 2026.2.3.
- The project is configured with the `Temurin-24` IntelliJ SDK and Java 24 language level, so the current IDE compiler JDK differs from both the Maven runtime and the project's Java 21 target. This is recorded as environment context, not a proposed compiler-setting change.
- Existing IDE guidance warns that IntelliJ partial compilation may differ from Maven and recommends Maven delegation; this does not demonstrate the native workflow required by this task.
- `PrefabProcessor` retains deduplication and aggregation state across rounds and writes global outputs once after deferred aggregate resolution. `JavaFileWriter` suppresses `FilerException` when a source type already exists, with ownership remembered only in a static map keyed by the current `ProcessingEnvironment`. AsyncAPI and migration metadata are also written as Filer resources.
- Confirmed dependency-tracking gap before implementation: `JavaFileWriter` called `Filer.createSourceFile` without originating elements, and `OutputTargetFileOutput.setPreferredElement` did not forward ownership to the production writer. `TestJavaFileWriter` also writes outside the Filer under `target/prefab-test-sources/`; its existing-output behavior must be assessed separately because documented overrides live under `src/test/java`.
- At the start of this work, no automated test exercised generated-file ownership or repeated incremental compilations. The baseline `mvn -q -pl annotation-processor -am test` passed; new ownership assertions now cover a per-model source and a shared generated source. Native IntelliJ failure evidence is still required before claiming that ownership propagation explains every stale-output scenario.
- Implemented originating-element propagation for core Java sources, shared generated sources, Avro outputs, event consumers, and generated resources. Aggregated outputs now list contributing model types where available; `TestJavaFileWriter`'s Filer fallback also forwards owners. A focused regression verifies both a per-model controller origin and all origins on a shared Mongo index configuration.
- The initial suite exposed an Avro compile error from accessing JavaPoet's private `TypeSpec.name`; the writer now uses the already-known schema type name. Follow-up `mvn -q -pl annotation-processor,avro-processor,kafka,pubsub,sns-sqs -am test` completed successfully.
- Planned native JPS scenario matrix (not yet run): in `examples/mongodb`, edit/add/rename/delete aggregate components and nested records; in `examples/avro`, edit/add/delete `@Avsc` event contracts and schema inputs; in `examples/kafka`, `examples/pubsub`, and `examples/sns-sqs`, edit/add/delete event handlers and topics. Trigger ordinary IntelliJ Build Project after each change and compare generated Java/resources with a clean Maven build.
- Native IntelliJ incremental edit/add/rename/delete scenarios and clean-build comparisons remain unverified. Do not remove the existing IDE guidance until those runs demonstrate the supported native workflow.
- Next, rerun tests across annotation-processor, Avro, Kafka, Pub/Sub and SNS/SQS; then document precise reproduction steps and either execute native IDEA scenarios or record the external IDE limitation.
<!-- SECTION:ANALYSIS:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Capture reproducible IntelliJ/compiler/JDK versions and scenarios for editing, adding, renaming and deleting model components, nested types and generated event inputs across modules.
- [ ] #2 Diagnose each failure with processor-round and file-ownership evidence; fix affected generation/cache/dependency behavior without changing compiler settings or disabling incremental compilation.
- [ ] #3 Incremental output matches a clean Maven build for supported scenarios and never silently retains obsolete artifacts or loses annotations.
- [ ] #4 Add automated lifecycle/generation regressions and a repeatable native IntelliJ verification procedure; describe any external IDE limitation honestly.
- [ ] #5 Update IDE guidance with the verified workflow; do not advertise universal support from a single successful rebuild.
<!-- AC:END -->

## Implementation Plan
<!-- SECTION:PLAN:BEGIN -->
Create a minimal reproducible multi-module fixture, trace originating elements and processor state over rounds, fix the demonstrated causes, and compare incremental and clean generated outputs.
<!-- SECTION:PLAN:END -->
