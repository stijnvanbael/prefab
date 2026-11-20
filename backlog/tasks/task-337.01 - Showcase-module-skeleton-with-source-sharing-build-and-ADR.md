---
id: TASK-337.01
title: Showcase module skeleton with source-sharing build and ADR
status: Done
assignee:
  - '@claude'
created_date: '2026-11-20 19:47'
labels:
  - examples
dependencies: []
parent_task_id: TASK-337
priority: high
type: task
ordinal: 172000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Create `examples/showcase` (Postgres + Kafka, JSON) with a minimal shop aggregate and one integration test, and prove the variant build mechanism end to end with a throw-away or first real variant.

Build mechanism (see TASK-337): `showcase` attaches `sources` and `test-sources` jars (maven-source-plugin `jar-no-fork` + `test-jar-no-fork`). A variant depends on those classifiers only (never on the main jar), unpacks them with maven-dependency-plugin into `target/generated-sources/showcase` and `target/generated-test-sources/showcase`, adds them with build-helper-maven-plugin, and runs prefab-annotation-processor on them. Put the shared variant build config in an `examples/showcase-variant-parent` pom so a variant pom only lists its dependencies.

Record the decision (single showcase, recompile per variant, capability-based gaps, rejected alternatives) as an ADR in `backlog/decisions/`.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 examples/showcase builds and its integration test passes on Postgres + Kafka
- [x] #2 A variant module containing only pom.xml and resources compiles the showcase sources and runs the showcase tests
- [x] #3 Variant build config is shared via a parent pom; a variant pom lists only its technology dependencies
- [x] #4 IntelliJ imports showcase and variants without shared-content-root warnings
- [x] #5 ADR added to backlog/decisions
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. `examples/showcase` (artifact `showcase-example`): Postgres + Kafka (JSON), `ShowcaseApplication`, minimal `catalog.Product` aggregate (`@Create`, `@GetById`) and `ProductIntegrationTest` using the generated `ProductClient`. Attaches `sources` + `test-sources` jars (maven-source-plugin, forced on despite `maven.source.skip` in the development profile; resources excluded so each variant owns its configuration).
2. `examples/showcase-variant-parent` (pom): depends on the two source classifiers only (provided scope, all transitives excluded so the reference stack never leaks into a variant), unpacks them into `target/generated-sources/showcase` and `target/generated-test-sources/showcase`, adds those plus `target/prefab-test-sources` as source roots and runs the Prefab annotation processor. Also carries the technology-neutral dependencies (prefab-core, annotation processor, prefab-test).
3. First real variant `examples/showcase-mongodb` (MongoDB + Kafka): pom with only prefab-mongodb + prefab-kafka, plus application.yml / application-test.yml. Proves the processor regenerates persistence code for a different classpath. TASK-337.08 completes it.
4. ADR decision-007 in `backlog/decisions/`.
5. Register modules in the root pom (showcase before variant parent before variants).
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Decision: `backlog/decisions/decision-007 - One-showcase-recompiled-per-technology-variant.md`.

**Modules** (registered in the root pom):
- `examples/showcase` (`showcase-example`): `ShowcaseApplication`, `catalog.Product` (`@Create`, `@GetById`), `ProductIntegrationTest` via the generated `ProductClient`.
- `examples/showcase-variant-parent` (pom): shared variant build, plus technology-neutral deps (prefab-core, annotation processor, prefab-test).
- `examples/showcase-mongodb` (`showcase-mongodb-example`): pom with prefab-mongodb + prefab-kafka, `application.yml`, `application-test.yml`. First real variant; TASK-337.08 completes it (capabilities file, replacing `examples/mongodb`).
- `.run/Showcase tests (<stack>).run.xml`: shared IntelliJ JUnit configurations, one per stack. The variant ones run Maven `generate-test-sources` on the variant before launch, then all tests in `be.appify.prefab.example.showcase` with the variant module's classpath. Every new variant needs one.

**How a variant gets the sources (deviates from the description above)**
- The variant copies `../showcase/src/main/java` and `../showcase/src/test/java` (maven-resources-plugin `copy-resources`) into `target/showcase/src/{main,test}/java`. There is no Maven dependency on the showcase module.
- First implementation: classifier jars `showcase-sources`/`showcase-test-sources` (maven-jar-plugin) unpacked by maven-dependency-plugin. It worked in Maven, but IntelliJ imports looked fine while tests could only be run on the Postgres module. A test run from its file always uses the module that owns it, and a variant only saw showcase changes after the showcase was built and installed. Copying makes the refresh a single goal on the variant (~4.5 s, it also generates the test clients) that the run configurations execute before launch. It also removes the risk that IntelliJ imports the classifier dependency as a module dependency.
- maven-source-plugin `sources`/`test-sources` jars were never an option: they contain every compile source root, including generated code.
- `TestJavaFileWriter` derives the project root from the `/src/main/java` path segment, hence the `target/showcase/src/main/java` layout: test clients go to `target/showcase/target/prefab-test-sources`, and manual test-client overrides in the showcase tests keep working.
- The variant cleans `target/showcase/src` at `initialize` (so deleted showcase sources don't linger) but keeps `prefab-test-sources`: the compiler skips an up-to-date main compile, so the processor would not regenerate deleted test clients.
- Resources are not shared: each variant owns its configuration. Technology-neutral resources (AVSC schemas, event baselines) will need copying too in TASK-337.04/TASK-337.07. Note that AvscPlugin, KafkaPlugin, EventSchemaFactoryWriter and AvscSnapshotPlugin read `Path.of("src/main/resources", ...)` relative to the working directory, which matters once those resources are shared.
- Bug fixed in prefab-test: with prefab-kafka on the classpath and no `@Event` yet there is no `EventRegistry` bean, so `KafkaTestAutoConfiguration.testConsumerFactory` failed the context. It now falls back to an empty registry.

**Verification**
- Full reactor `mvn install`: BUILD SUCCESS (all modules and examples).
- `ProductIntegrationTest` passes on Postgres + Kafka and on MongoDB + Kafka, on clean and incremental builds. The variant has no Postgres driver or Flyway, generates no migrations and starts a Mongo container. Adding/removing a showcase source propagates to the variant with `generate-test-sources` on the variant alone.
- IntelliJ import confirmed by the user (AC #4). The `.run/` configurations were written without an IDE at hand and still need a first try in IntelliJ.
<!-- SECTION:NOTES:END -->
