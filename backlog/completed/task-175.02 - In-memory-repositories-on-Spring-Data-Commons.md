---
id: TASK-175.02
title: In-memory repositories on Spring Data Commons
status: Done
assignee: []
created_date: '2026-11-23 11:00'
updated_date: '2026-11-23 15:00'
labels:
  - test
dependencies:
  - TASK-175.01
parent_task_id: TASK-175
priority: high
type: task
ordinal: 156200
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Back every generated repository with an in-memory store in in-memory mode (TASK-175 design §3, decision-011). Spike: KeyValue 4.1.3 has derived queries, paging, sorting and a settable `KeySpaceResolver`, but no Query by Example, no optimistic locking, and its configuration extension only claims `KeyValueRepository`/`@KeySpace` types in strict mode.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Every generated repository and `@RepositoryMixin` interface gets an in-memory bean, also with JDBC or MongoDB on the classpath (own registrar and repository factory bean)
- [x] #2 Derived queries, paging and sorting work, including on `Reference<T>` properties
- [x] #3 Query by Example supports the matchers the generated list services use (`exact`, `contains`, `startsWith`, `endsWith`, `regex`, `ignoreCase`, ignored paths)
- [x] #4 `@Version` is checked and incremented on records; a stale version raises `OptimisticLockingFailureException`, so the generated service returns `409 Conflict`
- [x] #5 All implementations of a sealed aggregate share the sealed root's keyspace
- [x] #6 In-memory `RangeQueryExecutor` and `FullTextQueryExecutor` beans exist; full-text matching and relevance ordering are documented as an approximation
- [x] #7 The processor emits runtime autocomplete metadata on generated `autocompleteBy*` methods and the in-memory backend honours scan mode and match strategy
- [x] #8 A method with a native JDBC `@Query` or Mongo `@Query`/`@Aggregation` logs a WARN at startup and throws `UnsupportedOperationException` naming the method and `@IntegrationTest(containers = true)` when called
<!-- AC:END -->

## Definition of Done
<!-- DOD:BEGIN -->
- [x] #1 All acceptance criteria are tested
- [x] #2 The build is green
- [x] #3 Code is clean (refactored)
<!-- DOD:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
- **Spring Data KeyValue was dropped after CI failed.** Its `spring.factories` registers a `RepositoryFactorySupport`, so `RepositoryConfigurationDelegate#multipleStoresDetected` found two store modules next to JDBC/MongoDB and switched to strict mode, in which only repositories of `@Table`/`@Document` types or store interfaces are claimed. Prefab's generated repositories vanished in containers mode (`NoSuchBeanDefinitionException` for `CashRegisterRepository` in `examples/avro`). There is no property to disable strict mode. The in-memory backend now depends only on `spring-data-commons`, and `NoSecondStoreModuleTest` asserts that no `RepositoryFactorySupport` is registered by `org.springframework.data.keyvalue` or `be.appify.prefab`.
- Package `be.appify.prefab.test.inmemory.persistence` in `prefab-test`, activated by `InMemoryPersistenceAutoConfiguration` (in-memory mode only, registered in `AutoConfiguration.imports`).
- `InMemoryRepositoriesRegistrar` scans the `AutoConfigurationPackages` for interfaces that extend `Repository` (skipping `@NoRepositoryBean` and generic interfaces) and registers an `InMemoryRepositoryFactoryBean` (a `RepositoryFactoryBeanSupport`) for each. `InMemoryRepositoryFactory` supplies `InMemoryPrefabRepository` as implementation and `InMemoryQueryLookupStrategy`.
- `InMemoryMappingContext` (`AbstractMappingContext`, `BasicPersistentEntity`, `AnnotationBasedPersistentProperty`) describes aggregates; JDK types are values, not entities. `AggregateKeySpaceResolver` maps all implementations of a sealed type to the sealed root. `InMemoryStore` holds a `ConcurrentHashMap` per key space with a `ReentrantLock` per key space.
- `InMemoryPrefabRepository` reads identifiers per runtime class through `AggregateProperties`, since the sealed root has no id property. `findAll(Example, Pageable)` is matched to the base class by signature; only that form of Query by Example exists, because the generated repositories declare only that one.
- Versions follow Spring Data JDBC: a new aggregate has a `null` wrapper version or `0` for a primitive, and is stored with version `0` (wrapper) or `1` (primitive); a stale or missing version on save or delete throws `OptimisticLockingFailureException`, a new aggregate with a taken id throws `DuplicateKeyException`. Records are copied with `InstantiationAwarePropertyAccessor`. Aggregates without `@Version` are overwritten.
- Derived queries: `DerivedRepositoryQuery` evaluates a `PartTree` with a `PartMatcher` per part. Keywords: simple/negating property, in/not in, like/not like, containing/not containing, starting/ending with, greater/less than (equal), before/after, between, is (not) null, true/false and regex, with `IgnoreCase`; any other keyword fails at startup naming `Repository#method`. Subjects `find/read/get/query/stream/count/exists/delete/remove` with `Distinct`, `Top`/`First` and `OrderBy`; `Pageable`/`Sort` parameters; `List`/`Collection`, `Page`, `Slice`, `Optional`, single entity, `Stream`, numeric count, `boolean` and delete results. A `String` argument for a `Reference` property is wrapped in a `Reference`, because generated methods such as `findByProduct(String, Pageable)` pass the identifier. Negations (`Not`, `NotIn`, `NotLike`, `NotContaining`) never match a null property, as in SQL.
- `InMemoryQueryLookupStrategy`: `@AutocompleteQuery` first (so the generated JDBC `@Query`/Mongo `@Aggregation` is never inspected), then native `@Query`/`@Aggregation` (matched by annotation class name) -> WARN at startup, `UnsupportedOperationException` naming `Repository#method` and `@IntegrationTest(containers = true)` on call, else a derived query.
- Processor: `AutocompleteRepositoryWriter` adds the runtime annotation `core` `AutocompleteQuery(property, scanMode, matchStrategy)` to the JDBC, Mongo and no-persistence variants. Covered by `AutocompleteRepositoryWriterTest`. Documented in `annotation-reference.md`.
- Autocomplete in memory: JDBC signature `(query, limit, offset)` receives the raw text; the Pageable signature with a full-text strategy receives the regular expression built by the generated MongoDB service (`TypeaheadQuery.valueRegex`), so it is matched as a case insensitive regex. `FUZZY` uses a trigram approximation of `pg_trgm` with threshold 0.3 (`word_similarity` for `PREFIX`, `similarity` for `CONTAINS`).
- `InMemoryRangeQueryExecutor`: numbers are compared by value, other values through `Comparable`; an aggregate without a value never matches. `InMemoryFullTextQueryExecutor` (see `TextSearch`): words of all searched properties, type-ahead prefixes and initials as in `TypeaheadQuery`, web search syntax (`"phrase"`, `-word`, `or`) for the rest, fuzzy via trigram similarity; relevance = sum of the `Weight.textIndexWeight()` of the properties that match each word. The approximation is documented in the Javadoc; user docs follow in TASK-175.05.
- Tests (no Docker): `InMemoryRepositoriesTest`, `QueryByExampleTest`, `InMemoryQueryExecutorsTest`, `AutocompleteRepositoryQueryTest`, `AggregateKeySpaceResolverTest`, `DerivedQueryTest` and `NoSecondStoreModuleTest` over fixtures in `inmemory/persistence/fixture`.
<!-- SECTION:NOTES:END -->
