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
2. **Each variant recompiles a copy of the hand-written showcase sources.**
   - `examples/showcase-variant-parent` copies `../showcase/src/main/java` and `../showcase/src/test/java` into
     `target/showcase/src/main/java` and `target/showcase/src/test/java` (maven-resources-plugin), registers those as
     source roots, and runs the Prefab annotation processor against the variant's own dependencies. There is no
     Maven dependency on the showcase module, so its reference stack (Postgres + Kafka) never leaks into a variant.
   - The copied tree deliberately mimics a module layout: the processor derives the project root from the
     `/src/main/java` segment, so generated test clients land in `target/showcase/target/prefab-test-sources` and
     manual test-client overrides in the showcase tests are still detected.
   - Before copying, the variant removes the previous copy (`target/showcase/src`), so sources deleted from the
     showcase do not linger. Generated test clients are kept, because the processor only regenerates them when the
     main sources are recompiled.
   - Refreshing a variant needs no showcase build: `mvn -f examples/showcase-<technology> generate-test-sources`
     takes seconds. The shared IntelliJ run configurations in `.run/` (`Showcase tests (<stack>)`) run that goal
     before launch and then run every showcase test with the variant's classpath.
   - Tests are written once and run on every variant. `@IntegrationTest` already selects Testcontainers from the
     classpath, so they need no per-variant code.
3. **Configuration is per variant.** Only Java sources are copied; every variant owns its complete configuration.
   Technology-neutral resources (Avro schemas, event contract baselines) will be copied the same way when the slices
   that need them land (TASK-337.04, TASK-337.07).
4. **Optional slices.** Features that cannot compile on every stack (Prefab Streams needs Kafka; Avro is chosen per
   event in source) live in optional source folders (`src/streams/java`, `src/avro/java`), which only supporting
   variants copy.
5. **Gaps are explicit, not silent.** Tests declare `@RequiresCapability(...)`, variants declare what they support;
   unsupported capabilities are reported as skipped tests with a reason and a backlog task, and the coverage matrix
   in `examples/README.md` is checked against the same files (TASK-337.02).

## Rejected alternatives

| Alternative | Why not |
|---|---|
| Variant depends on the compiled showcase jar | The jar holds code generated for Postgres + Kafka; the variant's technology is never exercised. |
| Run a compiled showcase test-jar against the variant | Generated APIs differ per classpath; drift shows up only as `NoSuchMethodError` or missing beans at runtime instead of at compile time. |
| `build-helper:add-source ../showcase/src/main/java` in each variant | IntelliJ forbids a content root shared by two modules; the variants would not import cleanly. |
| Showcase publishes its sources as classifier jars that variants unpack (first implementation) | Works in Maven, but in the IDE the variant only sees showcase changes after building and installing the showcase, so running the tests on a variant is slow and stale-prone. The dependency on the classifier jars may also be imported by IntelliJ as a module dependency, putting Postgres-generated classes on the variant's classpath. |
| maven-source-plugin `sources` / `test-sources` jars | Include generated source roots, which then clash with what the variant generates itself. |
| One module per technology with copied sources (status quo) | Copies drift apart and hide which features are missing on which stack. |
| Maven profiles switching dependencies of one module | Only one technology can be built per invocation, CI must run the module several times, and IDEs import one profile at a time. |

## Consequences

- Adding a technology is a pom plus configuration; every showcase test immediately runs on it.
- Showcase sources are compiled once per variant, so the build takes longer.
- In an IDE, sources are edited in `examples/showcase`; the copies under a variant's `target/` are generated sources.
  Running a test from its file runs it on the reference stack; the `.run/` configurations run the suite on a variant.
- A variant relies on the showcase sitting next to it at `../showcase`; it cannot be built from published artifacts.
  That is acceptable for examples that live in this repository only.
- `prefab-test` had to tolerate an application on Kafka without any `@Event` yet (no `EventRegistry` bean); the test
  consumer factory now falls back to an empty registry.
