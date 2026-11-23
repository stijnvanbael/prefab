---
id: TASK-175
title: Add in-memory persistence and messaging backend for tests
status: To Do
assignee: []
created_date: '2026-05-08 16:37'
updated_date: '2026-11-23 10:00'
labels: []
dependencies: []
priority: high
ordinal: 156000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add a `prefab-test-inmemory` module that replaces PostgreSQL/MongoDB with an in-memory repository and replaces Kafka/Pub/Sub/SNS with a synchronous in-process event bus. Allows integration tests that don't need real infrastructure to run without containers, greatly reducing feedback loop time.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 prefab-test-inmemory module ships a ConcurrentHashMap-backed repository implementation
- [ ] #2 An in-process synchronous event bus replaces Kafka/Pub/Sub/SNS in tests using this module
- [ ] #3 Existing integration tests can opt-in by swapping the persistence/messaging module dependency
- [ ] #4 The in-memory backend is documented in the Developer Guide
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [ ] #1 All acceptance criteria are tested
- [ ] #2 The build is green
- [ ] #3 Code is clean (refactored)
<!-- DOD:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
### Analysis (2026-11-23)

#### The constraint that shapes everything

The annotation processor generates code from the **compile** classpath (decision-007). An application that depends on
`prefab-postgres` + `prefab-kafka` gets JDBC repositories (with SQL `@Query` for `@Autocomplete`), `@KafkaListener`
consumers, Flyway scripts and so on. Tests compile against those same generated classes. So the in-memory backend
**cannot change what is generated**; it has to run the *already generated* artefacts against in-memory infrastructure
at test runtime. In practice "swapping the module" (AC #3) means *adding* a test-scoped `prefab-test-inmemory`
dependency; `prefab-postgres`/`prefab-kafka` stay on the classpath because main code needs them.

#### What the generated code needs at runtime

**Persistence**

| Generated artefact / contract | Source | What in-memory must provide |
|---|---|---|
| `{A}Repository extends CrudRepository<A,String>, PagingAndSortingRepository<A,String>` | `PersistenceWriter` | Repository bean per generated interface |
| `findBy<Parent>(String, Pageable)`, `findBy<TenantId>(String, Pageable)` | `PersistenceWriter`, `TenantPlugin` | Derived queries |
| `findAll(Example<A>, Pageable)` (`@GetList` with filters, tenant) | `GetListRepositoryWriter`, `GetListServiceWriter` | Query by Example in memory: `exact`, `contains`, `startsWith`, `endsWith`, `regex`, `ignoreCase`, `withIgnorePaths`, `matchingAll` |
| `RangeQueryExecutor` / `FullTextQueryExecutor` beans | core SPI (`core/spring/data`), impl in postgres/mongodb | In-memory implementations of both SPIs |
| `autocompleteBy<Field>` with JDBC `@Query` SQL or Mongo `@Aggregation` | `AutocompleteRepositoryWriter` | Intercept and evaluate in memory. `@Autocomplete` is `RetentionPolicy.SOURCE`, so scan mode / match strategy are **not** visible at runtime (see processor change below) |
| `@RepositoryMixin` derived queries (`findByCustomer`, `countByCategory`, …) | user code | Derived queries on `Reference<T>` (a record, equality works) |
| `@RepositoryMixin` with store-specific `@Query` (e.g. `examples/kafka` `UserStatusRepositoryMixin`) | user code | Cannot be executed. Fail with an actionable message (see below) |
| `@Multicast(queryMethod = …)` | `MulticastEventHandlerWriter` | Just calls a mixin query + `saveAll`; works if the query works |
| Polymorphic (sealed interface) repositories | `PersistenceWriter.writePolymorphicRepository` | One store per sealed root, not per concrete record class |
| `@Transactional` on every generated service | `ApplicationWriter` | A `PlatformTransactionManager` bean (no DataSource exists) |
| `@Version` → `OptimisticLockingFailureException` → `ConflictException` | service | Version check + increment on immutable records |
| `Binary` fields | `StorageService` (temp file) | Stored by reference; nothing extra |
| Clean state per test | `PersistenceTestExecutionListener` calls `deleteAll()` on every `CrudRepository` | Works unchanged |

**Messaging**

| Path | Today | What in-memory must provide |
|---|---|---|
| Publish | `EventPublisher` → `SpringDomainEventPublisher` → matching `DomainEventDispatcher` beans (`GenericKafkaProducer`, `GenericPubSubPublisher`, `GenericSnsPublisher`); topics from the platform-neutral `EventRegistry.topicsForDispatch` | One `InMemoryEventBus implements DomainEventDispatcher` claiming every registered type; the broker dispatchers must not be present |
| Consume, Kafka | `{A}KafkaConsumer` `@Component` with `@KafkaListener(topics = …)` methods `on<Event>(event[, @Header(EventType.HEADER) String eventType])` | Discover listener methods and invoke them per topic. Without Prefab's `KafkaConfiguration`/`@EnableKafka` the annotation is inert, so the bean still starts |
| Consume, Pub/Sub | `{A}PubSubSubscriber` constructor calls `PubSubUtil.subscribe(SubscriptionRequest)` | `PubSubUtil` is a **concrete** class needing `PubSubAdmin` and subscriber templates; generated code depends on it directly |
| Consume, SQS | `{A}SqsSubscriber` constructor calls `SqsUtil.subscribe(SqsSubscriptionRequest)` | Same problem as Pub/Sub |
| Non-`@Event` events | `ApplicationEventPublisher` / `@EventListener` | Unchanged |
| `@TestEventConsumer EventConsumer<T>` | `TestConsumerExecutionListener` (Kafka), `TestSubscriberExecutionListener` (Pub/Sub), `TestSqsSubscriberExecutionListener`; each activates on **class presence** | An in-memory listener; the broker listeners must stand down |

**prefab-test today** starts containers whenever the store/broker classes are present (`PostgresTestAutoConfiguration`
`@ConditionalOnClass(Table.class)`, `KafkaTestcontainerAutoConfiguration`, `MongoDbTestAutoConfiguration`,
`PubSubTestAutoConfiguration`, `SnsTestAutoConfiguration`). In in-memory mode none of them may activate.

#### Proposed design

1. **Module** `test-inmemory` (`prefab-test-inmemory`). Depends on `prefab-test` (reuses `@IntegrationTest`,
   `EventConsumer`, `@TestEventConsumer`, `EventAssertions`/`EventConsumerAssert`) and `spring-data-keyvalue`.
2. **Activation.** The module's `EnvironmentPostProcessor` sets `prefab.test.infrastructure=in-memory` (default
   `containers`). prefab-test's container auto-configurations and broker test listeners get a condition on that
   property. An `AutoConfigurationImportFilter` (registered by the module) filters out store/broker
   auto-configurations in in-memory mode: DataSource, Flyway, Data JDBC, Data MongoDB, Kafka, GCP Pub/Sub, AWS
   SNS/SQS, plus Prefab's `PrefabJdbcConfiguration`, `PrefabMongoConfiguration`, `KafkaConfiguration`,
   `PubSubConfiguration`, `SnsConfiguration`. A filter is preferred over `spring.autoconfigure.exclude`, which would
   overwrite the user's own excludes. Exact Boot 4.1 class names to be confirmed during implementation.
3. **Repositories** on Spring Data KeyValue (`MapKeyValueAdapter` = `ConcurrentHashMap`, satisfies AC #1):
   - Own `ImportBeanDefinitionRegistrar` + `RepositoryConfigurationExtension` scanning the auto-configuration
     packages. Needed because with JDBC/Mongo jars on the classpath Spring Data runs in *strict* mode, and generated
     repositories (plain `CrudRepository`, aggregates without `@KeySpace`) would not be claimed by KeyValue.
   - Base class `InMemoryPrefabRepository` (extends `SimpleKeyValueRepository`) adding Query by Example
     (`findAll/count/exists(Example…)`), optimistic locking for `@Version` on records (check, then save a copy with
     the incremented version through the mapping context's property accessor), and keyspace = sealed root for
     polymorphic aggregates.
   - Own `QueryLookupStrategy`: derived queries → KeyValue's SpEL derivation; `autocompleteBy<Field>` → in-memory
     autocomplete; a method carrying a store-specific `@Query` (JDBC) or `@Query`/`@Aggregation` (Mongo) → bean is
     created, a WARN is logged at startup, and **invocation** throws `UnsupportedOperationException` naming the method
     and the remedy (run that test on containers). Failing lazily keeps unrelated tests green.
   - In-memory `RangeQueryExecutor` (comparisons on `Comparable` properties) and `FullTextQueryExecutor`
     (case-insensitive word-prefix match per searched property, i.e. the Mongo type-ahead semantics; relevance =
     weighted number of matched words when the sort is empty or `relevance`; fuzzy = trigram similarity against
     `similarityThreshold`). Documented as an approximation of Postgres ranking.
   - `PlatformTransactionManager`: a resource-less manager that supports `TransactionSynchronization` (needed for
     after-commit delivery). In-memory writes are **not** rolled back; documented limitation.
   - **Processor change**: emit a runtime-retained metadata annotation (e.g. `@AutocompleteQuery(property,
     scanMode, matchStrategy)` in `core`) on generated `autocompleteBy*` methods so the in-memory backend can honour
     the declared strategy instead of guessing from the method name.
4. **Event bus** `InMemoryEventBus implements DomainEventDispatcher`:
   - `canDispatch` = type known to `EventRegistry` (any platform); `topicOverrides` honoured.
   - Subscribers per topic: Kafka via a `BeanPostProcessor` that collects `@KafkaListener` methods and resolves
     topics (placeholders and SpEL through the bean expression resolver); generic bindings get the `EventType` id as
     the header argument. Pub/Sub and SQS via the subscription SPI below.
   - Delivery: synchronous and in-process, FIFO per thread (a trampoline: events published by a handler are queued
     and delivered after the current handler returns, so ordering is deterministic and recursion bounded). When a
     transaction is active, delivery starts after commit; otherwise immediately. A handler exception is logged and
     recorded, not propagated into the publishing request; a test execution listener fails the test that caused
     unhandled handler failures. Retries, dead-lettering and `@EventHandlerConfig` are not emulated.
   - Effect for tests: when a MockMvc call returns, every cascaded handler has run, so `EventConsumerAssert…within()`
     passes on the first poll and no test code changes.
   - Events are passed by reference (no serialization round-trip) in the first version.
   - `InMemoryTestConsumerExecutionListener` injects `@TestEventConsumer` fields by subscribing an `EventConsumer`
     to the resolved topic and resets it per test.
5. **Pub/Sub and SQS subscription SPI (core + processor change).** Extract narrow interfaces from `PubSubUtil` and
   `SqsUtil` (e.g. `PubSubSubscriptions.subscribe(SubscriptionRequest<T>)`, `SqsSubscriptions.subscribe(...)`),
   implemented by the existing classes, and make `PubSubSubscriberWriter`/`SqsSubscriberWriter` depend on the
   interface. The in-memory module provides implementations that forward to the bus. Rejected: subclassing
   `PubSubUtil`/`SqsUtil` with dummy cloud clients (fragile, couples to constructor internals). Under decision-009
   both utils are internals (not documented as replaceable), so the change is allowed; extracting an interface they
   implement is backward-compatible anyway.
6. **decision-009 check.** Rule 1 forbids reflection on internals *for extension points*. The Kafka discovery reads
   Spring's public `@KafkaListener` annotation on Prefab-generated beans; both sides are owned by Prefab, so this is
   an internal coupling, not an extension point. It must be pinned by tests that compile a consumer with the real
   processor. Alternative if that coupling is unwanted: let the processor also register every generated handler
   with a platform-neutral subscription SPI, which would unify Kafka, Pub/Sub and SQS (larger processor change).

#### Out of scope (explicit gaps, to document)

- Prefab Streams (Kafka Streams backend or `streams-engine` wiring to the bus): follow-up task.
- User `@RepositoryMixin` methods with native `@Query`/`@Aggregation`.
- Database constraints (unique indexes, foreign keys, column sizes), Flyway/Mongo migration validation.
- Rollback of in-memory writes; Avro/JSON serialization; broker retries/DLT; `@AsyncCommit`.
- Application code that injects `DataSource`, `JdbcTemplate`, `MongoTemplate`, `KafkaTemplate` directly.

#### Verification

- Unit tests in `test-inmemory` per component (QBE matcher, version conflicts, polymorphic keyspace, range/full-text
  executors, autocomplete strategies, bus ordering/trampoline/after-commit/failure reporting, topic SpEL resolution,
  generic event header).
- New showcase variant `examples/showcase-inmemory` (decision-007): `prefab-postgres` + `prefab-kafka` +
  `prefab-test-inmemory`, capabilities file declaring the gaps (STREAMS, KAFKA_STREAMS_BREAKOUT and any other that
  fails, each with a reason and task). All other showcase tests must pass **with Docker unavailable** (proves no
  container starts). Record wall-clock time against the reference showcase in the notes (input for TASK-343).
- Pub/Sub and SNS/SQS: an in-memory run of the pubsub/sns-sqs examples or variants once those exist.

#### Documentation

`modules.md` (module row, feature mapping), `feature-guides.md` (testing: when to use in-memory vs containers,
semantics and gaps), `configuration.md` (`prefab.test.infrastructure`), `troubleshooting.md` (unsupported `@Query`
message, failed handler reported on a test), `annotation-reference.md` (new runtime autocomplete metadata),
`examples/README.md` coverage matrix. ADR `decision-011` for activation model and delivery semantics.

#### Risks to spike first

1. Spring Data strict mode: KeyValue claiming generated repositories while JDBC/Mongo jars are present.
2. Spring Data KeyValue 4.x (Boot 4.1) API: custom base class, keyspace resolution for sealed roots, records with
   `@Version`, derived queries on `Reference<T>`.
3. Excluding Boot 4.1 auto-configurations by filter without breaking `PrefabCoreConfiguration` conditions
   (`@ConditionalOnBean(PrefabCoreConfiguration.class)` everywhere).
A spike could not run in the analysis session: Maven Central answered HTTP 429.

#### Proposed refinement of the acceptance criteria

- AC #3 → "A project opts in by adding the test-scoped `prefab-test-inmemory` dependency next to `prefab-test`; main
  persistence/messaging dependencies stay; no container is started."
- Add: "`@TestEventConsumer` and `EventConsumerAssert` work unchanged."
- Add: "A repository method that cannot run in memory fails with a message naming the method and the remedy."
- Add: "The showcase passes on an in-memory variant without Docker, gaps declared as capabilities."

#### Proposed split

1. Activation, prefab-test gating, auto-configuration filter, transaction manager.
2. In-memory repositories: KeyValue registrar, QBE, versioning, polymorphic, range/full-text executors, autocomplete
   (with the processor metadata change).
3. In-memory event bus for Kafka + `@TestEventConsumer` listener.
4. Pub/Sub and SQS subscription SPI + in-memory implementations.
5. `examples/showcase-inmemory`, docs, ADR, timing measurement.

#### Open decisions (owner)

- D1 Activation: module presence switches the whole test module to in-memory, overridable by property
  (recommended), or a per-class annotation so container and in-memory tests mix in one module.
- D2 Delivery: synchronous after commit with failures recorded and reported on the test (recommended), or inline
  with exceptions propagating into the publishing request.
- D3 Serialization round-trip of events: off in v1 (recommended) or on, to catch serialization bugs.
- D4 Pub/Sub and SQS: subscription SPI in this task (recommended, AC #2 names them) or Kafka first, the rest as a
  follow-up.
- D5 Repository technology: Spring Data KeyValue (recommended, pending spike) or an own repository factory.

#### Related

TASK-174 (`@PrefabSlice`, mocked repository) is complementary: a slice for one aggregate's HTTP layer, this task
for full-context, cross-aggregate, event-driven tests. TASK-186 intends to use this backend for the playground.
TASK-343 should measure the impact.
<!-- SECTION:PLAN:END -->

## Comments

<!-- COMMENTS:BEGIN -->
created: 2026-11-20 20:14
---
AI-driven development: container-free integration tests let agents (especially cheap models like Haiku) verify event-driven behaviour quickly and without Docker. The Haiku benchmark (TASK-343) should measure the impact.
---
<!-- COMMENTS:END -->
