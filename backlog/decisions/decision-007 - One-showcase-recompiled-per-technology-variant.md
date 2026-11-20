---
id: decision-007
title: One showcase application, recompiled per technology variant
date: '2026-11-20'
status: accepted
related_tasks:
  - TASK-337
  - TASK-337.01
  - TASK-337.02
---

## Context

`examples/` held six independent applications with five unrelated domains. Two of them (`pubsub`, `sns-sqs`) were
near-identical copies of the same chat domain. Readers could not see what Prefab offers as a whole, and gaps in
technology coverage were invisible: a feature that only works on Postgres simply had no example elsewhere.

The goal is one shop application, `examples/showcase`, on a reference stack (Postgres + Kafka, JSON), plus variants
that prove the *same* sources work on other technologies (MongoDB, Google Pub/Sub, AWS SNS/SQS).

What makes this harder than sharing a library: **the annotation processor decides what to generate from the compile
classpath.** `DbMigrationPlugin` only writes Flyway migrations when Spring Data Relational is present,
`MongoIndexPlugin` needs `MongoTemplate`, `ControllerUtil` reacts to Spring Security and springdoc, and
`@Event(platform = DERIVED)` resolves the messaging platform from the classpath. A compiled showcase jar is
therefore Postgres + Kafka code; a variant cannot reuse it.

## Decision

1. **One showcase, configuration-only variants.** `examples/showcase` holds all Java sources and tests. A variant
   module (`examples/showcase-<technology>`) contains a pom listing its technology dependencies, its configuration
   (`application.yml`, `application-test.yml`) and, from TASK-337.02 on, a capabilities file. No Java.
2. **Each variant recompiles the hand-written showcase sources.**
   - `showcase` attaches two extra jars with maven-jar-plugin: classifier `showcase-sources` (`src/main/java`) and
     `showcase-test-sources` (`src/test/java`). These contain hand-written sources only. The regular
     `sources`/`test-sources` jars of maven-source-plugin are unusable: they include every compile source root, so
     the Postgres-generated code and generated test clients would end up in the variant twice.
   - `examples/showcase-variant-parent` depends on those two classifiers only (`provided`, all transitive
     dependencies excluded, so the reference stack never leaks into a variant), unpacks them into
     `target/showcase/src/main/java` and `target/showcase/src/test/java`, registers those as source roots, and runs
     the Prefab annotation processor. The unpacked tree deliberately mimics a module layout: the processor derives
     the project root from the `/src/main/java` segment, so generated test clients land in
     `target/showcase/target/prefab-test-sources` and manual test-client overrides in the showcase tests are still
     detected.
   - Before unpacking, the variant removes the previous copy (`target/showcase/src` and the unpack markers), so
     sources deleted from the showcase do not linger. Generated test clients are kept, because the processor only
     regenerates them when the main sources are recompiled.
   - Tests are written once and run on every variant. `@IntegrationTest` already selects Testcontainers from the
     classpath, so they need no per-variant code.
3. **Configuration is per variant.** The source jars exclude resources; every variant owns its complete
   configuration. Technology-neutral resources (Avro schemas, event contract baselines) will get their own
   classifier when the slices that need them land (TASK-337.04, TASK-337.07).
4. **Optional slices.** Features that cannot compile on every stack (Prefab Streams needs Kafka; Avro is chosen per
   event in source) live in optional source folders (`src/streams/java`, `src/avro/java`) published under their own
   classifiers, which only supporting variants unpack.
5. **Gaps are explicit, not silent.** Tests declare `@RequiresCapability(...)`, variants declare what they support;
   unsupported capabilities are reported as skipped tests with a reason and a backlog task, and the coverage matrix
   in `examples/README.md` is checked against the same files (TASK-337.02).

## Rejected alternatives

| Alternative | Why not |
|---|---|
| Variant depends on the compiled showcase jar | The jar holds code generated for Postgres + Kafka; the variant's technology is never exercised. |
| Run a compiled showcase test-jar against the variant | Generated APIs differ per classpath; drift shows up only as `NoSuchMethodError` or missing beans at runtime instead of at compile time. |
| `build-helper:add-source ../showcase/src/main/java` in each variant | IntelliJ forbids a content root shared by two modules; the variants would not import cleanly. |
| maven-source-plugin `sources` / `test-sources` jars | Include generated source roots, which then clash with what the variant generates itself. |
| One module per technology with copied sources (status quo) | Copies drift apart and hide which features are missing on which stack. |
| Maven profiles switching dependencies of one module | Only one technology can be built per invocation, CI must run the module several times, and IDEs import one profile at a time. |

## Consequences

- Adding a technology is a pom plus configuration; every showcase test immediately runs on it.
- Showcase sources are compiled once per variant: the build takes longer, and variants must be built after
  `showcase` (guaranteed by the dependency on its source classifiers).
- In an IDE, variants are edited through `examples/showcase`; the unpacked copies under `target/` are generated
  sources. A variant picks up showcase changes after a Maven build of `showcase` (or of the whole reactor).
- Unpacking also runs, harmlessly, in `showcase-variant-parent` itself, because Maven executes inherited plugin
  bindings on pom modules too.
- `prefab-test` had to tolerate an application on Kafka without any `@Event` yet (no `EventRegistry` bean); the test
  consumer factory now falls back to an empty registry.
