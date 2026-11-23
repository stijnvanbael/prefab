---
id: decision-011
title: In-memory backend as the default for integration tests
date: '2026-11-23'
status: accepted
related_tasks:
  - TASK-175
  - TASK-174
  - TASK-343
---

## Context

Every `@IntegrationTest` starts Testcontainers for the stores and brokers on the classpath (Postgres, MongoDB, Kafka,
Pub/Sub emulator, LocalStack). That makes the feedback loop slow and makes Docker a prerequisite, which hurts humans
and, even more, agents that iterate often (TASK-343).

Prefab generates code from the compile classpath (decision-007): an application on Postgres + Kafka contains JDBC
repositories with SQL queries and `@KafkaListener` consumers. A test backend therefore cannot change the generated
code; it has to run the already generated artefacts against other infrastructure at test runtime.

## Decision

1. **In-memory is the default.** `@IntegrationTest` runs on ConcurrentHashMap-backed repositories and an in-process
   event bus. `@IntegrationTest(containers = true)` runs a test class on Testcontainers as before. The property
   `prefab.test.containers=true` sets that default for a whole test module, because configuration-only showcase
   variants cannot change annotations. The attribute wins over the property; the two modes get separate cached
   Spring contexts.
2. **The backend lives in `prefab-test`.** It is the default and reuses the test API (`EventConsumer`,
   `@TestEventConsumer`, assertions); a separate module would form a dependency cycle with `prefab-test`.
3. **Repositories build on Spring Data KeyValue** (`MapKeyValueAdapter`, derived queries, paging and sorting).
   Prefab adds what KeyValue lacks: registration of the generated repositories despite Spring Data's strict mode,
   Query by Example, optimistic locking for `@Version`, one keyspace per sealed aggregate, the full-text and range
   query executors and autocomplete. Native `@Query`/`@Aggregation` methods fail when called, naming the method and
   the `containers = true` remedy.
4. **Events are delivered synchronously and in-process, after commit** of the publishing transaction (immediately
   without one), in FIFO order per thread. Handler failures are recorded and fail the test that triggered them; they
   do not propagate into the publishing request.
5. **Events are serialized and deserialized** with the platform's own serializers (Kafka JSON/Avro with the mock
   schema registry, Pub/Sub, SNS/SQS), including version headers and migrations. Encoding happens at publish time, so
   serialization errors surface where a real producer would raise them. Header and attribute handling is shared
   between the real transports and the bus.
6. **Kafka, Pub/Sub and SNS/SQS are all covered.** Pub/Sub and SQS subscribers move to a narrow subscription
   interface that both the cloud implementation and the in-memory bus implement.

## Rejected alternatives

| Alternative | Why not |
|---|---|
| Opt-in by adding a separate `prefab-test-inmemory` dependency | Keeps the slow path as the default; dependency cycle with `prefab-test` |
| Inline delivery with exceptions propagating to the publisher | A handler bug would show up as a failure of an unrelated HTTP call; real brokers decouple the two |
| Passing events by reference | Misses serialization, versioning and migration bugs, which are a large part of what container tests catch |
| An own Spring Data repository factory | Rewrites derived-query evaluation, sorting and paging that KeyValue already provides |
| Subclassing `PubSubUtil`/`SqsUtil` with dummy cloud clients | Couples the test backend to constructor internals of cloud clients |
| An embedded database (H2) instead of maps | Still needs schema handling and diverges from Postgres SQL anyway; does not solve MongoDB |

## Consequences

- Most integration tests run without Docker and much faster; cascaded event handlers have run when a MockMvc call
  returns, so `EventConsumerAssert…within()` passes on the first poll.
- **Behaviour change on upgrade:** existing `@IntegrationTest`s run in memory. Tests that depend on native queries,
  database constraints, migrations or broker retries need `containers = true`. Release notes and the troubleshooting
  guide say so.
- In-memory writes are not rolled back, constraints are not enforced and full-text ranking is approximated. These
  gaps are documented and declared as capabilities in the showcase.
- The repository's reference showcase and technology variants keep running on containers through
  `prefab.test.containers`; a configuration-only in-memory variant proves the default.
- Prefab Streams is not covered by the in-memory bus yet; that is a follow-up.
