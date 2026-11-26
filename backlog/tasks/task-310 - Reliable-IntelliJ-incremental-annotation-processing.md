---
id: TASK-310
title: >-
  Make IntelliJ IDEA incremental compilation reliable without compiler-setting
  workarounds
status: In Progress
assignee: []
created_date: '2026-11-16T15:43:00Z'
updated_date: '2026-11-26T09:00:00Z'
labels:
  - bug
  - dx
  - annotation-processor
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
- Execution plan (2026-11-25): the native IntelliJ scenarios need a real IDE, so they run from GitHub Copilot's agent
  inside IntelliJ IDEA rather than from a cloud session. Write the scenario matrix as a self-contained runbook
  (steps, expected generated files, a result table per scenario, IDE/JDK/Maven versions) so the IDE agent can follow
  it and report evidence, then fold the results back into this task. The README guidance changes only after those
  results.
<!-- SECTION:ANALYSIS:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Capture reproducible IntelliJ/compiler/JDK versions and scenarios for editing, adding, renaming and deleting model components, nested types and generated event inputs across modules.
- [x] #2 Diagnose each failure with processor-round and file-ownership evidence; fix affected generation/cache/dependency behavior without changing compiler settings or disabling incremental compilation.
- [ ] #3 Incremental output matches a clean Maven build for supported scenarios and never silently retains obsolete artifacts or loses annotations.
- [x] #4 Add automated lifecycle/generation regressions and a repeatable native IntelliJ verification procedure; describe any external IDE limitation honestly.
- [ ] #5 Update IDE guidance with the verified workflow; do not advertise universal support from a single successful rebuild.
<!-- AC:END -->

## Native IntelliJ Verification Runbook

Run in IntelliJ IDEA with annotation processing enabled and **Maven delegation off** (`Settings > Build, Execution,
Deployment > Build Tools > Maven > Runner > Delegate IDE build/run actions to Maven` unchecked); restore it afterwards.
Do not change other compiler settings. Record the IDEA build number, the JPS javac version, and `mvn -v`.

1. Baseline: `git stash -u` any probe edits, **Build > Rebuild Project**, then copy `examples/showcase/target/` to a
   scratch directory as `native/`. In a separate clone at the same commit run
   `mvn -q -pl examples/showcase -am install -DskipTests` and copy its `target/` as `maven/`. Compare with
   `diff -r` on `generated-sources/annotations`, `prefab-test-sources`, `classes/db/migration`,
   `classes/META-INF/async-api` (compare parsed JSON; channel order may differ) and the list of `.class` paths.
2. For each scenario below, make the edit in the IDE, run the stated action, compare against a clean Maven build of
   the same edit in the second clone, then revert the edit and rebuild.

| # | Scenario (examples/showcase unless noted)                                    | IDE action                                          |
|---|-------------------------------------------------------------------------------|-----------------------------------------------------|
| 1 | Change `@Size(max)` on `Category.name` only                                    | Build Project                                       |
| 2 | Change `@Size(max)` on the `Category` `@Create` constructor parameter only     | Build Project                                       |
| 3 | Add a nested `@Event` record in a new source file                              | Recompile `src/main/java`, then Build Project        |
| 4 | Rename that record                                                             | Recompile `src/main/java`, then Build Project        |
| 5 | Delete that source file                                                        | Recompile main source roots (incl. `src/avro`, `src/streams*`), then Build Project |
| 6 | Rename a field in an `.avsc` used by an `@Avsc` contract                       | Build Project, then Recompile the contract interface |
| 7 | Add a `@RepositoryMixin` method; then edit only the aggregate                  | Build Project (twice)                               |

Pass criteria per scenario: generated Java, test helpers, migration SQL (all aggregates present), AsyncAPI channels
and class paths match the Maven reference; no obsolete file remains in `target/prefab-test-sources`. Record a second
Build Project when the first reports a missing test helper (known JPS discovery lag, not stale output).

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
Create a minimal reproducible multi-module fixture, trace originating elements and processor state over rounds, fix the demonstrated causes, and compare incremental and clean generated outputs.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
2026-11-25 IntelliJ verification: project SDK is Temurin 24 with language level JDK_24; Maven CLI is 3.9.11 on Temurin JDK 25 and project target remains Java 21. IntelliJ's local MavenRunner had delegateBuildToMaven=true; this was temporarily disabled only for one native IDE rebuild and restored to true afterward. The IntelliJ run configuration for JavaFileWriterTest delegated to Maven and passed all 4 tests, so it is not evidence of native JPS test execution.

Native incremental probe: changed Category.name @Size max from 100 to 101, invoked IntelliJ source-only build and full rebuild, but generated CreateCategoryRequest.java remained max=100. After a controlled deletion of that one generated file, IntelliJ build still did not recreate it; Maven compile restored the generated file with the source's original max=100 after the source was reverted. Thus generated-output freshness is not yet demonstrated and the result must not be reported as a successful native workflow.

With Maven delegation temporarily off, IntelliJ full rebuild failed with 27 errors: 13 generated Pub/Sub test event registrars in streams-pubsub reference package-private test classes from a subpackage, and the showcase-inmemory/showcase-mongodb variants report missing avro/shipment-dispatched.avsc plus consequent missing generated ShipmentDispatched/ShipmentService types. The build also reports existing deprecation warnings. Reproduction requires the current native IDE compiler profile and module setup; the exact compiler round/Filer event is not surfaced by the build tool, so root-cause attribution still needs processor-level evidence.

Workspace and source are restored (delegateBuildToMaven=true; Category @Size max=100). No tracked source edits remain. Do not remove the README Maven-delegation caution; native incremental output and the full IDE build are not yet reliable.

Correction and implementation plan: Category's @Create constructor has its own @Size(max=100). The earlier record-component-only edit did not establish a stale CreateCategoryRequest; the generated-file deletion was a delegated experiment, not a native incremental one. Compare a constructor edit with a clean Maven build, repair the reproduced native registrar visibility and variant resource failures, then exercise add/rename/delete on real IDEA Make with unchanged compiler settings.

IDEA 2026.3 (263.7307.200), native javac 24: the registrar accessibility and Avro lookup fixes allow a full native rebuild. Editing the @Create constructor to 101 and adding IncrementalProbe.Added regenerates the correct request, builder and registrar. A subsequent native Make with only Added renamed to Renamed matched all 132 main generated Java files in an isolated clean showcase Maven build. Not yet parity: AsyncAPI lost unrelated channels, the nested probe's assert/mother landed in target/classes and its conformance test was absent; the migration manifest order differed. Investigate aggregation across partial inputs, test-source ownership, and resource-only invalidation, then repeat rename/delete against separate clean output.

Scope decision: keep this processor-only; for a resource-only Avro edit use native Build Project (copy resources) and Recompile the owning @Avsc contract. A controlled trackingNumber → trackingCode edit after a clean native baseline copied the schema but did not invoke javac or update the record. An IntelliJ recompile of ShipmentEvent.java alone invoked javac 24 and regenerated trackingCode. The earlier build that also rebuilt Java did not isolate this case. On the nested rename, parsed AsyncAPI matched the clean reference (channel ordering differed); it was the earlier incremental addition that omitted eleven unrelated channels. The new test helper writer uses Elements.getFileObjectOf (available since Java 18); the nested assert/mother/conformance sources now all land in target/prefab-test-sources. Five writer tests passed via native IDEA JUnit.

Deletion-only scope decision: preserve test-source and resource paths. If Make runs only the test compiler after deleting an owner, recompile the affected module's production source roots, then Build Project. On the initial run JPS removed the main builder/registrar but javac received no production roots; stale filesystem-written helpers broke test compilation and the shared AsyncAPI output disappeared. Implement owner-based cleanup/aggregation, not a blanket clean or an IntelliJ plugin.

Resumed: a full native rebuild now gets past processor compilation; its seven errors name the unindexed Renamed helper/factory from the earlier probe. Check the actual processor path and establish a baseline by recreating the source before attributing those errors to current reconciliation. Compare complete generated trees in separate directories, not just build exit codes.

Indexed native rebuild and explicit deletion recovery verified (IDEA 2026.3/JPS javac 24). Recompile examples/showcase/src/{main,avro,streams,streams-kafka}/java together, then Build Project: no probe Java or class remains; helper and main Java match clean Maven, as do AsyncAPI (15 unrelated channels), migration and baseline resources, and class paths. During ordinary Make an aggregate was reprocessed without its repository mixin source; the generated interface lost its mixin and eight references failed. Indexing source-owned repository/mixin relationships eliminated that failure. Full native rebuild and the focused 34 Maven tests pass.

Additional boundary measured: ordinary Build Project after a nested add and @Create edit emits the correct 132 main sources, 120 helpers, 16-channel AsyncAPI, baselines and migrations, but JPS does not discover six new test .class files until the next Make. Renaming the event in a new Make also updates the shared Assertions.java before JPS picks up the new assert helper, producing one missing-symbol error; a subsequent ordinary Make succeeds. This is distinct from stale generated output. Do not claim first-pass test compilation parity.

Rename comparison after the second Make found a separate data-integrity failure: the native migration manifest and V1__generated.sql contained only 6 of 12 aggregates. Java sources/helpers, all class paths, event baselines and the 16-channel AsyncAPI matched. Do not publish the partial migration. JPS can remove the CLASS_OUTPUT copy of a shared resource before invoking javac; reading a manifest only via CLASS_PATH is not an authoritative completeness check.

Decision: for edits that add, rename or remove generated helper filenames, preserve paths and support native Recompile of the affected module's production sources followed by Build Project. Retain ordinary Build Project for Java edits that keep the generated path set stable. Separately fix migration completeness: SQL must never be inferred from only the aggregates observed in a partial javac invocation.

Migration inputs now persist the desired tables and index/extension metadata by source owner, not the rendered SQL. Native Rebuild matched the clean Maven showcase; changing BOTH Category.name and its @Create constructor max from 100 to 101 via ordinary native Build Project produced VARCHAR (101), @Size(max=101), all 12 aggregate names, 15 channels and full file parity. Next verify constructor-only and helper path changes independently, run a consecutive-javac regression, and include the dependent test accessor in the Avro comparison.

After rebasing on a9b1d9bc, JPS Make recompiled 19 processor sources (not just Category). JPS removed PrefabProcessor.class before invoking javac while the AutoService-generated META-INF/services/javax.annotation.processing.Processor remained. ServiceLoader then failed before processing started. Pin the bootstrap processor for the processor module only in its Maven model; verify a native recompile of PrefabProcessor before rerunning the constructor-only trial.
2026-11-26 (Claude Code, resumed after Copilot ran out of credits): committed the owner-indexed aggregation work
(SourceContributions, IncrementalBuildState — renamed from TestSourceInventory because it also holds the mixin and
input indexes — owner-reconciled test sources, migration inputs, AsyncAPI inputs, assertion entries). Added
IncrementalCompilationTest, which runs consecutive javac invocations over an on-disk module the way JPS does:
partial recompile after the shared migration was deleted keeps every aggregate; partial recompile keeps unrelated
AsyncAPI channels; deleting a source removes its channel and its generated assert; an unchanged recompile does not
rewrite test helpers. All four fail on the pre-fix code and pass now. Recorded decision-013. Documented the workflow
in README, troubleshooting and generated-artefacts §6.15. The IDE probe edits (Category max=101, IncrementalProbe)
are not committed. Still open for AC #3/#5: native scenarios 2, 4 and 7 of the runbook have not been rerun since the
migration fix; the README and troubleshooting table describe the workflow observed so far and must be corrected if
those runs disagree.

2026-11-26 follow-up probes: the local Maven incremental compiler verified the three remaining scenario shapes, but
these are not native JPS results. A constructor-only `@Create` edit regenerated `CreateCategoryRequest` with
`@Size(max=101)`. Adding an event source generated its assert, mother and conformance helpers; renaming the source
generated only the renamed helpers and registrar, with no obsolete `IncrementalProbe*` files left behind. Adding a
`ProductRepositoryMixin` method followed by an aggregate edit, with two consecutive compiles, retained the mixin on
the generated repository. `IncrementalCompilationTest` passes in isolation. The full annotation-processor Maven suite
still has an unrelated Windows failure in `AgentContextPluginTest` caused by a `/C:/...` path. Native IntelliJ/JPS
scenarios 2, 4 and 7 therefore remain unverified.
<!-- SECTION:NOTES:END -->
