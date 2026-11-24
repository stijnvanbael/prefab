---
id: TASK-175
title: Add in-memory persistence and messaging backend for tests
status: Done
assignee: []
created_date: '2026-05-08 16:37'
updated_date: '2026-11-24 09:30'
labels: []
dependencies: []
priority: high
ordinal: 156000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Make `@IntegrationTest` run on an in-memory backend by default: ConcurrentHashMap-backed repositories replace
PostgreSQL/MongoDB and a synchronous in-process event bus replaces Kafka, Pub/Sub and SNS/SQS. Events still go
through the platform's own serializers, so as much behaviour as possible is covered without containers. Tests that
need real infrastructure opt in with `@IntegrationTest(containers = true)`. This greatly reduces the feedback loop
for humans and agents and removes the Docker dependency from most test runs.

Decisions: [decision-011](../decisions/decision-011%20-%20In-memory-backend-as-the-default-for-integration-tests.md).
Work is split into TASK-175.01 to TASK-175.05.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 `@IntegrationTest` runs on an in-memory backend by default: every generated repository is backed by a ConcurrentHashMap (Prefab's own repository factory) and no container is started
- [x] #2 A synchronous in-process event bus replaces Kafka, Pub/Sub and SNS/SQS; every event is serialized and deserialized with the platform's own serializers (JSON and Avro, version headers and migrations included)
- [x] #3 `@IntegrationTest(containers = true)` runs a test class on Testcontainers as today; `prefab.test.containers=true` sets that default for a whole test module
- [x] #4 `@TestEventConsumer` and `EventConsumerAssert` work unchanged in both modes
- [x] #5 A repository method that cannot run in memory fails when called, with a message naming the method and `@IntegrationTest(containers = true)`; a failing event handler fails the test that triggered it
- [x] #6 The showcase test suite passes in memory without Docker; gaps are declared as capabilities
- [x] #7 The in-memory backend is documented in the Developer Guide
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 All acceptance criteria are tested
- [x] #2 The build is green
- [x] #3 Code is clean (refactored)
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
### Decisions (2026-11-23, owner)

| # | Question | Decision |
|---|---|---|
| D1 | Activation | In-memory is the **default** for `@IntegrationTest`; containers are opted into with an attribute on `@IntegrationTest`. A module-wide property is added for configuration-only showcase variants (decision-007), which cannot change annotations |
| D2 | Event delivery | Synchronous. Interpreted as: in-process, after commit of the publishing transaction, handler failures recorded and reported on the test (not propagated into the publishing request) |
| D3 | Serialization | Round-trip every event through the platform serializers, to cover as much ground as possible without containers |
| D4 | Pub/Sub and SQS | In scope |
| D5 | Repository technology | Own repository factory on `spring-data-commons` (initially Spring Data KeyValue, dropped in TASK-175.02; rationale below) |

Recorded in decision-011.

**D5 rationale.** The first spike chose `spring-data-keyvalue` 4.1.x (`MapKeyValueAdapter`, derived queries, paging).
It has no Query by Example and no optimistic locking, which Prefab would add itself. CI then showed the decisive
problem: KeyValue's `spring.factories` registers a `RepositoryFactorySupport`, so Spring Data counts two store modules
next to JDBC or MongoDB and enters strict mode, where only repositories of `@Table`/`@Document` types or of
store-specific interfaces are claimed. Prefab's generated repositories are neither, so they vanish in containers mode,
the default. There is no property to turn strict mode off. Prefab therefore implements the repository factory, derived
queries and mapping context itself on `spring-data-commons`, and registers the repositories programmatically, without
any `spring.factories` entry.

### Analysis

#### The constraint that shapes everything

The annotation processor generates code from the **compile** classpath (decision-007). An application that depends on
`prefab-postgres` + `prefab-kafka` gets JDBC repositories (with SQL `@Query` for `@Autocomplete`), `@KafkaListener`
consumers, Flyway scripts and so on. Tests compile against those same generated classes. So the in-memory backend
**cannot change what is generated**; it runs the *already generated* artefacts against in-memory infrastructure at
test runtime. Main dependencies (`prefab-postgres`, `prefab-kafka`, …) stay on the classpath.

#### What the generated code needs at runtime

**Persistence**

| Generated artefact / contract | Source | What in-memory must provide |
|---|---|---|
| `{A}Repository extends CrudRepository<A,String>, PagingAndSortingRepository<A,String>` | `PersistenceWriter` | Repository bean per generated interface |
| `findBy<Parent>(String, Pageable)`, `findBy<TenantId>(String, Pageable)` | `PersistenceWriter`, `TenantPlugin` | Derived queries |
| `findAll(Example<A>, Pageable)` (`@GetList` with filters, tenant) | `GetListRepositoryWriter`, `GetListServiceWriter` | Query by Example in memory: `exact`, `contains`, `startsWith`, `endsWith`, `regex`, `ignoreCase`, `withIgnorePaths`, `matchingAll` |
| `RangeQueryExecutor` / `FullTextQueryExecutor` beans | core SPI (`core/spring/data`), impl in postgres/mongodb | In-memory implementations of both SPIs |
| `autocompleteBy<Field>` with JDBC `@Query` SQL or Mongo `@Aggregation` | `AutocompleteRepositoryWriter` | Intercept and evaluate in memory. `@Autocomplete` is `RetentionPolicy.SOURCE`, so scan mode / match strategy are **not** visible at runtime (processor change below) |
| `@RepositoryMixin` derived queries (`findByCustomer`, `countByCategory`, …) | user code | Derived queries on `Reference<T>` (a record, equality works) |
| `@RepositoryMixin` with store-specific `@Query` (e.g. `UserStatusRepositoryMixin` in `examples/kafka`, `pubsub`, `sns-sqs`) | user code | Cannot be executed; fail with an actionable message |
| `@Multicast(queryMethod = …)` | `MulticastEventHandlerWriter` | Calls a mixin query + `saveAll`; works if the query works |
| Polymorphic (sealed interface) repositories | `PersistenceWriter.writePolymorphicRepository` | One keyspace per sealed root, not per concrete record class |
| `@Transactional` on every generated service | `ApplicationWriter` | A `PlatformTransactionManager` bean (no DataSource exists) |
| `@Version` (Spring Data annotation on records) → `OptimisticLockingFailureException` → `ConflictException` | service | Version check + increment on immutable records |
| `Binary` fields | `StorageService` (temp file) | Stored by reference; nothing extra |
| Clean state per test | `PersistenceTestExecutionListener` calls `deleteAll()` on every `CrudRepository` | Works unchanged |

**Messaging**

| Path | Today | What in-memory must provide |
|---|---|---|
| Publish | `EventPublisher` → `SpringDomainEventPublisher` → matching `DomainEventDispatcher` beans (`GenericKafkaProducer`, `GenericPubSubPublisher`, `GenericSnsPublisher`); topics from the platform-neutral `EventRegistry.topicsForDispatch` | One `InMemoryEventBus implements DomainEventDispatcher` claiming every registered type; the broker dispatchers must not be present |
| Serialization | Kafka `DynamicSerializer`/`DynamicDeserializer` (headers, versions, migrations; Avro needs a schema registry, `KafkaSchemaRegistrySupport` already supports Confluent's mock registry); Pub/Sub `PubSubSerializer`/`PubSubDeserializer`; SNS/SQS `SnsSerializer`/`SqsDeserializer`. All are constructible from `EventRegistry`, `ConversionService`, `JsonUtil` (+ `KafkaProperties`) | Reused as-is; attribute/header stamping that now lives in the publishers and `PubSubUtil`/`SqsUtil` must be shared |
| Consume, Kafka | `{A}KafkaConsumer` `@Component` with `@KafkaListener(topics = …, groupId = …)` methods `on<Event>(event[, @Header(EventType.HEADER) String eventType])` | Discover listener methods and invoke them per topic. Without Prefab's `KafkaConfiguration`/`@EnableKafka` the annotation is inert, so the bean still starts |
| Consume, Pub/Sub | `{A}PubSubSubscriber` constructor calls `PubSubUtil.subscribe(SubscriptionRequest)` | `PubSubUtil` is a **concrete** class needing `PubSubAdmin` and subscriber templates; generated code depends on it directly |
| Consume, SQS | `{A}SqsSubscriber` constructor calls `SqsUtil.subscribe(SqsSubscriptionRequest)` | Same problem as Pub/Sub |
| Non-`@Event` events | `ApplicationEventPublisher` / `@EventListener` | Unchanged |
| `@TestEventConsumer EventConsumer<T>` | `TestConsumerExecutionListener` (Kafka), `TestSubscriberExecutionListener` (Pub/Sub), `TestSqsSubscriberExecutionListener`; each activates on **class presence** | An in-memory listener; the broker listeners must stand down |

**prefab-test today** starts containers whenever the store/broker classes are present (`PostgresTestAutoConfiguration`
`@ConditionalOnClass(Table.class)`, `KafkaTestcontainerAutoConfiguration`, `MongoDbTestAutoConfiguration`,
`PubSubTestAutoConfiguration`, `SnsTestAutoConfiguration`). In in-memory mode none of them may activate.

### Design

1. **Location: inside `prefab-test`** (package `be.appify.prefab.test.inmemory`), not a separate module. It is the
   default, and it reuses `@IntegrationTest`, `EventConsumer`, `@TestEventConsumer` and the assertions; a separate
   `prefab-test-inmemory` would depend on `prefab-test` while `prefab-test` needs it as default (cycle). New
   dependency: `spring-data-commons` (version from the Boot BOM); not `spring-data-keyvalue`, see D5.
2. **Mode selection.**
   - `@IntegrationTest(containers = true)`: a `ContextCustomizerFactory` (registered in `spring.factories`) adds
     `prefab.test.containers=true` with highest precedence. The customizer takes part in the context cache key, so
     container and in-memory test classes in one module get separate cached contexts.
   - Otherwise `prefab.test.containers` from the test configuration decides (default `false`). Configuration-only
     showcase variants set it in `application-test.yml`.
   - In-memory mode: an `AutoConfigurationImportFilter` (`EnvironmentAware`) drops store/broker transport
     auto-configurations: Boot DataSource, JDBC, Flyway, Data JDBC, MongoDB, Data MongoDB, Kafka, GCP Pub/Sub,
     AWS SNS/SQS; Prefab's `PrefabJdbcConfiguration`, `PrefabMongoConfiguration`, `KafkaConfiguration`,
     `PubSubConfiguration`, `SnsConfiguration`; prefab-test's container auto-configurations.
     `PrefabRegistryConfiguration` (the `EventRegistry`) stays. A test asserts every listed class exists, so a Boot
     rename fails the build instead of silently starting a container. A filter is preferred over
     `spring.autoconfigure.exclude`, which would overwrite the user's own excludes.
   - prefab-test's broker test listeners and `MongoDbTestExecutionListener` skip unless containers mode is on.
3. **Repositories (own factory on Spring Data Commons).**
   - Own `ImportBeanDefinitionRegistrar` that finds the generated repository interfaces in the auto-configuration
     packages and registers an own `RepositoryFactoryBeanSupport` for each. The factory has no `spring.factories`
     entry (a regression test guards this).
   - An own mapping context and a keyspace resolver that maps every implementation of a sealed aggregate to the
     sealed root's keyspace; a `ConcurrentHashMap` per keyspace in `InMemoryStore`.
   - Base class `InMemoryPrefabRepository` adds CRUD, paging and sorting, Query by Example
     (`findAll(Example, Pageable)`, evaluated with `ExampleMatcherAccessor` and a property accessor) and optimistic
     locking: compare the stored `@Version` under a per-keyspace `ReentrantLock`, store a copy with the incremented
     version (records via the instantiating property accessor), throw `OptimisticLockingFailureException` on a
     mismatch. New-entity detection matches JDBC.
   - Own `QueryLookupStrategy`: derived queries -> own `PartTree` evaluation; `autocompleteBy<Field>` -> in-memory
     autocomplete; a method with a JDBC `@Query` or Mongo `@Query`/`@Aggregation` -> the bean is created, a WARN is
     logged at startup, and **invocation** throws `UnsupportedOperationException` naming the method and
     `@IntegrationTest(containers = true)`. Failing lazily keeps unrelated tests green.
   - In-memory `RangeQueryExecutor` (comparisons on `Comparable` properties) and `FullTextQueryExecutor`
     (case-insensitive word-prefix match per searched property, the Mongo type-ahead semantics; relevance = weighted
     number of matched words when the sort is empty or `relevance`; fuzzy = trigram similarity against
     `similarityThreshold`). Documented as an approximation of Postgres ranking.
   - Resource-less `PlatformTransactionManager` supporting `TransactionSynchronization` (needed for after-commit
     delivery). In-memory writes are **not** rolled back; documented limitation.
   - **Processor change:** emit a runtime-retained metadata annotation (e.g. `@AutocompleteQuery(property,
     scanMode, matchStrategy)` in `core`) on generated `autocompleteBy*` methods, so the in-memory backend honours the
     declared strategy instead of guessing from the method name.
4. **Event bus** `InMemoryEventBus implements DomainEventDispatcher`.
   - `canDispatch` = type known to `EventRegistry` (any platform); `topicOverrides` honoured.
   - **Encode at publish time**, inside the publishing call, with the codec of the event's platform: topics from
     `EventRegistry` (or overrides), key from `keyFor`, payload and headers/attributes from the platform serializer.
     A serialization error therefore surfaces in the publishing request, as with a real producer.
   - **Codecs.** Kafka: `DynamicSerializer.serialize(topic, headers, event)` /
     `DynamicDeserializer.deserialize(topic, headers, bytes)`; Avro uses the mock schema registry (in-memory mode sets
     `spring.kafka.properties.prefab.mock-schema-registry.enabled=true` unless a registry URL is configured).
     Pub/Sub and SNS/SQS: their serializers plus the message attributes the real path sets. The attribute/header
     stamping now spread over `GenericPubSubPublisher`, `GenericSnsPublisher`, `PubSubUtil` and `SqsUtil` moves into
     a per-platform message codec in `core` used by both the real transports and the bus (refactor, no behaviour
     change), so the in-memory path runs the same code. The in-memory configuration builds the serializer beans
     itself because the platform configurations are filtered out (`KafkaProperties` via
     `@EnableConfigurationProperties`).
   - **Delivery** is synchronous and in-process: after commit of the publishing transaction, or immediately without
     one. FIFO per thread (a trampoline: events published by a handler are queued and delivered after the current
     handler returns), so ordering is deterministic and recursion bounded. Every subscriber decodes its own copy.
     One delivery per listener method; generated group IDs are unique per owner and event, which matches consumer
     group semantics.
   - **Failures.** A handler exception is logged and recorded, not propagated into the publishing request. An
     execution listener fails the test that left recorded failures, then clears them. An
     `UnknownEventTypeException` is ignored, as real consumers do. Retries, dead-lettering and the retry part of
     `@EventHandlerConfig` are not emulated; `consumeFromTopics` is honoured because it is part of the listener topics.
   - **Effect for tests:** when a MockMvc call returns, every cascaded handler has run, so
     `EventConsumerAssert…within()` passes on the first poll and no test code changes.
   - **Subscribers.** Kafka: a `BeanPostProcessor` collects `@KafkaListener` methods and resolves topics
     (placeholders and SpEL through the bean expression resolver); generic bindings get the `EventType` id header.
     Pub/Sub and SQS: the subscription SPI below.
   - `InMemoryTestConsumerExecutionListener` injects `@TestEventConsumer` fields by subscribing an `EventConsumer` to
     the resolved topic (decoded like any subscriber) and resets it per test.
5. **Pub/Sub and SQS subscription SPI (core + processor change).** Extract narrow interfaces from `PubSubUtil` and
   `SqsUtil` (e.g. `PubSubSubscriptions.subscribe(SubscriptionRequest<T>)`, `SqsSubscriptions.subscribe(...)`),
   implemented by the existing classes; `PubSubSubscriberWriter`/`SqsSubscriberWriter` depend on the interface. The
   in-memory implementations forward to the bus. Rejected: subclassing `PubSubUtil`/`SqsUtil` with dummy cloud
   clients (fragile). Under decision-009 both utils are internals, and an extracted interface is
   backward-compatible anyway.
6. **decision-009 check.** Rule 1 forbids reflection on internals *for extension points*. The Kafka discovery reads
   Spring's public `@KafkaListener` annotation on Prefab-generated beans; both sides are owned by Prefab, so this is
   an internal coupling, pinned by tests that compile a consumer with the real processor.

### Rollout and migration impact

- The default flips only in TASK-175.05. Until then `prefab.test.containers` defaults to `true`, so the repository
  stays green while repositories and the bus are built.
- Existing adopters: after upgrading, `@IntegrationTest`s run in memory. Tests that rely on native queries,
  database constraints or broker retries need `containers = true` (release note + troubleshooting entry).
- In this repository: `examples/kafka`, `examples/pubsub`, `examples/sns-sqs` use native `@Query` mixins
  (`UserStatusRepositoryMixin`); the tests that call them get `containers = true`. The reference showcase and the
  existing showcase variants set `prefab.test.containers: true` to keep real-technology coverage; a new
  configuration-only `examples/showcase-inmemory` variant runs in memory. The 31 benchmark task tests switch to
  in-memory (input for TASK-343).

### Out of scope (explicit gaps, to document)

- Prefab Streams (Kafka Streams backend or `streams-engine` wiring to the bus): follow-up task.
- User `@RepositoryMixin` methods with native `@Query`/`@Aggregation`.
- Database constraints (unique indexes, foreign keys, column sizes), Flyway/Mongo migration validation.
- Rollback of in-memory writes; broker retries/DLT; `@AsyncCommit`.
- Application code that injects `DataSource`, `JdbcTemplate`, `MongoTemplate`, `KafkaTemplate` directly.

### Verification

- Unit tests per component (mode resolution and context caching, auto-configuration filter names, QBE matcher,
  version conflicts, sealed keyspace, range/full-text executors, autocomplete strategies, codecs per platform incl.
  Avro and a version migration, bus ordering/trampoline/after-commit/failure reporting, topic SpEL resolution,
  generic event header).
- `examples/showcase-inmemory` passes **with Docker unavailable** (proves no container starts), gaps declared in its
  capabilities file. Record wall-clock time against the reference showcase in the notes.
- Pub/Sub and SNS/SQS examples run in memory, except tests marked `containers = true`.

### Documentation

`modules.md`, `feature-guides.md` (testing: in-memory vs containers, semantics, gaps), `configuration.md`
(`prefab.test.containers`), `annotation-reference.md` (`@IntegrationTest(containers)`, runtime autocomplete
metadata), `troubleshooting.md` (unsupported `@Query`, failed handler reported on a test), `examples/README.md`
coverage matrix.

### Subtasks

| Task | Scope | Depends on |
|---|---|---|
| TASK-175.01 | Mode switch: `@IntegrationTest(containers)`, `prefab.test.containers`, auto-configuration filter, gating of container auto-configs and listeners, transaction manager | - |
| TASK-175.02 | In-memory repositories on Spring Data Commons, incl. autocomplete metadata in the processor | 175.01 |
| TASK-175.03 | In-memory event bus with Kafka serialization + `@TestEventConsumer` | 175.01 |
| TASK-175.04 | Pub/Sub and SNS/SQS: subscription SPI, shared message codecs, in-memory subscriptions | 175.03 |
| TASK-175.05 | Flip the default, migrate examples, `showcase-inmemory` variant, docs, timing | 175.02, 175.03, 175.04 |

### Related

TASK-174 (`@PrefabSlice`, mocked repository) is complementary: a slice for one aggregate's HTTP layer, this task for
full-context, cross-aggregate, event-driven tests. TASK-186 intends to use this backend for the playground.
TASK-343 should measure the impact.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
`@IntegrationTest` now runs in memory by default and `examples/showcase-inmemory` proves it without Docker. Delivered in five subtasks:

| Task | PR | Summary |
|---|---|---|
| TASK-175.01 | #218 | Mode switch: `@IntegrationTest(containers)`, `prefab.test.containers`, auto-configuration filter, gating of container auto-configurations and listeners, resource-less transaction manager |
| TASK-175.02 | #221 | In-memory repositories on Spring Data Commons (own factory after Spring Data KeyValue turned out to trigger strict repository detection), derived queries, Query by Example, optimistic locking, range, full-text and autocomplete, runtime `@AutocompleteQuery` metadata from the processor |
| TASK-175.03 | #219 | In-memory event bus with the Kafka serializers (JSON and Avro with mock registry), `@KafkaListener` subscription, `@TestEventConsumer`, handler failures failing the test |
| TASK-175.04 | #222 | Pub/Sub and SNS/SQS: subscription SPI, shared message codecs, in-memory subscriptions |
| TASK-175.05 | this PR | Default flipped, examples kept on containers, `showcase-inmemory`, documentation, timing |

- Outcome of the first end-to-end run (TASK-175.05): three bugs fixed in `prefab-test` (a null candidate in the auto-configuration filter, `Reference` identifiers not matching String ids, a configured registry URL overriding the mock registry). The showcase passes in memory with 5 skipped tests: `STREAMS`, `KAFKA_STREAMS_BREAKOUT` (TASK-372) and `SCHEMA_REGISTRY_API` (TASK-373).
- Timing (caveat: different machines): in-memory variant 21.9 s for the module including compilation, 14.1 s of test time, no Docker; reference showcase 24.1 s in CI run 45338081735 on `main`.
- Documented in `backlog/docs/feature-guides.md` section 7.20 (semantics, known differences, upgrading), plus `configuration.md`, `annotation-reference.md`, `troubleshooting.md`, `modules.md` and `examples/README.md`.
- Follow-ups: TASK-372 (Prefab Streams on the in-memory bus), TASK-373 (schema subjects in memory). TASK-343 (benchmark) should measure the impact on agents; the benchmark tasks now run in memory except `mixin-custom-sql`.
<!-- SECTION:NOTES:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-11-20 20:14
---
AI-driven development: container-free integration tests let agents (especially cheap models like Haiku) verify event-driven behaviour quickly and without Docker. The Haiku benchmark (TASK-343) should measure the impact.
---
<!-- COMMENTS:END -->
