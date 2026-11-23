---
id: TASK-175.02
title: In-memory repositories on Spring Data KeyValue
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
Back every generated repository with Spring Data KeyValue (`MapKeyValueAdapter`) in in-memory mode (TASK-175 design §3, decision-011). Spike: KeyValue 4.1.3 has derived queries, paging, sorting and a settable `KeySpaceResolver`, but no Query by Example, no optimistic locking, and its configuration extension only claims `KeyValueRepository`/`@KeySpace` types in strict mode.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Every generated repository and `@RepositoryMixin` interface gets an in-memory bean, also with JDBC or MongoDB on the classpath (own registrar with `KeyValueRepositoryFactoryBean`)
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
- New dependency `spring-data-keyvalue` (4.1.2, Boot BOM) in `prefab-test`. New package `be.appify.prefab.test.inmemory.persistence`, activated by the new `InMemoryPersistenceAutoConfiguration` (in-memory mode only, registered in `AutoConfiguration.imports`).
- `InMemoryRepositoriesRegistrar` scans the `AutoConfigurationPackages` for interfaces that extend `Repository` (skipping `@NoRepositoryBean` and generic interfaces) and registers an `InMemoryRepositoryFactoryBean` (a `KeyValueRepositoryFactoryBean`) for each, so it works with JDBC or MongoDB on the classpath. The factory bean needs `queryCreator` and `queryType` set although `InMemoryRepositoryFactory` ignores them.
- `InMemoryPrefabRepository` is an own base class (not `SimpleKeyValueRepository`): the sealed root has no id property, so identifiers are read per runtime class through `AggregateProperties`. `findAll(Example, Pageable)` is matched by Spring Data to the base class by signature; only that form of Query by Example exists, because the generated repositories only declare that one.
- `InMemoryStore` writes through the `MapKeyValueAdapter` with a `ReentrantLock` per key space. Versions follow Spring Data JDBC: a new aggregate has a `null` wrapper version or `0` for a primitive, and is stored with version `0` (wrapper) or `1` (primitive); a stale or missing version on save or delete throws `OptimisticLockingFailureException`, a new aggregate with a taken id throws `DuplicateKeyException`. Records are copied with `InstantiationAwarePropertyAccessor`. Aggregates without `@Version` are overwritten.
- `AggregateKeySpaceResolver` maps all implementations of a sealed type to the sealed root, walking up through nested sealed types.
- Derived queries run in `KeyValuePartTreeQuery` with `PredicateQueryCreator` (SpEL does not read record accessors). `ReferenceAwarePredicateQueryCreator` wraps a `String` argument in a `Reference` when the property is a `Reference`, because generated methods like `findByProduct(String, Pageable)` pass the identifier.
- `InMemoryQueryLookupStrategy`: `@AutocompleteQuery` first (so the generated JDBC `@Query`/Mongo `@Aggregation` is never inspected), then native `@Query`/`@Aggregation` (matched by annotation class name) -> WARN at startup, `UnsupportedOperationException` naming `Repository#method` and `@IntegrationTest(containers = true)` on call, else a derived query.
- Processor: `AutocompleteRepositoryWriter` adds the runtime annotation `core` `AutocompleteQuery(property, scanMode, matchStrategy)` to the JDBC, Mongo and no-persistence variants. Covered by `AutocompleteRepositoryWriterTest`. Documented in `annotation-reference.md`.
- Autocomplete in memory: JDBC signature `(query, limit, offset)` receives the raw text; the Pageable signature with a full-text strategy receives the regular expression built by the generated MongoDB service (`TypeaheadQuery.valueRegex`), so it is matched as a case insensitive regex. `FUZZY` uses a trigram approximation of `pg_trgm` with threshold 0.3 (`word_similarity` for `PREFIX`, `similarity` for `CONTAINS`).
- `InMemoryRangeQueryExecutor`: numbers are compared by value, other values through `Comparable`; an aggregate without a value never matches. `InMemoryFullTextQueryExecutor` (see `TextSearch`): words of all searched properties, type-ahead prefixes and initials as in `TypeaheadQuery`, web search syntax (`"phrase"`, `-word`, `or`) for the rest, fuzzy via trigram similarity; relevance = sum of the `Weight.textIndexWeight()` of the properties that match each word. Documentation of the approximation is in the Javadoc; user docs follow in TASK-175.05.
- Tests (`mvn -pl test test`, 180 tests, no Docker): `InMemoryRepositoriesTest`, `QueryByExampleTest`, `InMemoryQueryExecutorsTest`, `AutocompleteRepositoryQueryTest`, `AggregateKeySpaceResolverTest` over fixtures in `inmemory/persistence/fixture`.
<!-- SECTION:NOTES:END -->
