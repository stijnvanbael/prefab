---
id: task-030
title: Full text filter
status: Done
assignee: []
created_date: '2025-10-10 13:39'
updated_date: '2025-12-23 14:59'
labels:
  - "\U0001F4E6feature"
dependencies: []
ordinal: 468.75
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
@Filter(operator = FULL_TEXT, parameter = "query")

Multiple fields can be searched on with the same request parameter
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 `Filter.Operator.FULL_TEXT` exists and `@Filter` has a `parameter()` attribute (default: field name) that names the request parameter
- [x] #2 Several fields annotated with `@Filter(operator = FULL_TEXT, parameter = "query")` share one request parameter `query` and a single match is OR-ed across all of those fields
- [x] #3 Only a `String` request parameter is exposed (also in the `<Aggregate>Query` fluent API and `getList` signature); when absent, the filter is ignored
- [x] #4 FULL_TEXT is combined (AND) with other filters (EQUAL, CONTAINS, ranges), tenant and parent filters
- [x] #5 PostgreSQL: matches through `to_tsvector(...) @@ websearch_to_tsquery(...)` over the concatenated annotated columns; a GIN index is generated in the Flyway/Liquibase migration
- [x] #6 MongoDB: matches through `$text` with a text index generated on the annotated fields
- [x] #7 Compile error when FULL_TEXT is used on a non-String field, when fields sharing a `parameter` mix FULL_TEXT with another operator, when `parameter` clashes with another filter/request parameter name, or when an aggregate declares more than one distinct FULL_TEXT parameter
- [x] #8 `ignoreCase` has no effect for FULL_TEXT (documented); the language-neutral `simple` text search configuration is used (TASK-298 makes it configurable)
- [x] #9 Fuzziness (TASK-296), relevance scoring (TASK-297), language configuration (TASK-298) and prefix matching (TASK-299) are out of scope, but the design leaves room for them
- [x] #10 Tested with processor tests plus PostgreSQL and MongoDB integration tests; Developer Guide updated
<!-- AC:END -->

## Implementation Notes

### Analysis (2026-10-16)

**Current state**
- `@Filter` (core, `SOURCE` retention, repeatable) only carries `operator` + `ignoreCase`; the request parameter is always the field name (`GetListUtil.FilterManifest.parameterName()`, suffixed for range operators).
- Non-range filters are executed through Query by Example (`ExampleMatcher`), which can only AND per-property matchers. It cannot express OR across fields or database full-text predicates.
- Range filters (task-178) solved the same limitation with `RangeQueryExecutor` + `RangeCondition` (core) and `JdbcRangeQueryExecutor` / `MongoRangeQueryExecutor`; `GetListServiceWriter.findWithRanges` and `GetListPlugin` (registers the `RangeQueryExecutor` dependency, validates types) are the hook points. Full-text should reuse this pattern.

**Design**
1. *Annotation*: add `FULL_TEXT` to `Filter.Operator` and `String parameter() default ""` to `@Filter` (empty = field name). Ignored by the other operators except that it could later also be used to rename them; keep it documented as "request parameter name".
2. *Processor*: `FilterManifest` gets a `parameter` name; `parameterName()` returns it for FULL_TEXT. Add `GetListUtil.fullTextFiltersOf(filters)` which groups FULL_TEXT filters by parameter into `FullTextFilterManifest(parameter, List<fieldName>)`. A group yields ONE method parameter / query property of type `String`. `exampleFiltersOf` must exclude FULL_TEXT (as it does for ranges) and the ignore-paths logic must treat those fields as unfiltered by example. Validation errors per AC #7 in `GetListPlugin`.
3. *Core*: new `FullTextCondition(List<String> properties, String query)` (`isActive()` = non-blank query). Rather than a third executor, generalise: rename/extend `RangeQueryExecutor.findAll(example, ranges, pageable)` to accept a list of conditions (`QueryCondition` sealed interface with `RangeCondition` and `FullTextCondition`), or add an overload `findAll(example, ranges, fullTexts, pageable)`. Preferred: sealed `QueryCondition` + rename to `ConditionQueryExecutor` is cleaner but a breaking rename of an unreleased internal; decide with the tech lead. Minimum: add the overload to keep the generated code for existing users unchanged.
4. *PostgreSQL* (`JdbcRangeQueryExecutor`): Spring Data JDBC `Criteria` has no full-text support, so use `Criteria.CriteriaStep`'s unsafe/`Criteria.where(...)` is insufficient. Options: (a) `Query` + `CriteriaDefinition` custom, (b) build SQL with `NamedParameterJdbcOperations` + `RowMapper` via the mapping context, (c) Spring Data JDBC `Criteria` + `Functions`/`SqlIdentifier` expression through `JdbcAggregateOperations` is not supported. Spike required (see risks); expected result is a predicate `to_tsvector('simple'|config, coalesce(a,'') || ' ' || coalesce(b,'')) @@ websearch_to_tsquery(config, :query)`. Always bind the query as a parameter (no SQL injection); `websearch_to_tsquery` never throws on user input, unlike `to_tsquery`.
5. *Schema*: `DbMigrationWriter` already emits indexes for `@Indexed`; add a GIN index `idx_<table>_<parameter>_fts` on the same expression the executor uses (expression must match textually for the planner to use it). Mongo: `MongoIndexWriter` adds one compound text index on the annotated fields. MongoDB allows only ONE text index per collection, so two different FULL_TEXT parameters on one aggregate is a compile error on Mongo (validate, or merge into one index and document).
6. *MongoDB* (`MongoRangeQueryExecutor`): `TextCriteria.forDefaultLanguage().matchingAny/matching(query)` added with `Query.addCriteria`; `$text` can't be nested in `$or`, so combining with other criteria is plain AND, which fits AC #4. Note sorting by text score is out of scope.
7. *Tenant/parent*: both go through the example, which remains in place, so AC #4 holds without extra work.
8. *Other generated artefacts to update*: `GetListQueryWriter` (fluent query, uses `filterParamType`/`parameterName` – works once FilterManifest is grouped), `GetListControllerWriter` (`@RequestParam`), `GetListTestClientWriter`, OpenAPI module (parameter docs), `Filter` Javadoc, Developer Guide.

**Risks / open questions**
- Spring Data JDBC cannot express `@@` via `Criteria`; the Postgres executor needs a spike (custom SQL via `NamedParameterJdbcOperations` plus entity mapping is the likely fallback, and must still support paging/sorting and the example criteria).
- Text search configuration: use `simple` (language neutral, predictable) by default vs. the DB default config. Recommended: `simple` now; make configurable in the follow-up. Prefix matching (`foo:*`) is not offered by `websearch_to_tsquery`, so "type-ahead" behaviour is a follow-up.
- Mongo single-text-index limitation (see 5).
- Existing databases: the migration generation for existing aggregates adds the GIN index in a new migration (check `dbmigration` diffing handles expression indexes).

### Decisions (2026-10-16)
- Limit to **one FULL_TEXT parameter per aggregate** (also sidesteps MongoDB's single text index per collection).
- **New executor, composable with ranges**: `FullTextQueryExecutor.findAll(example, FullTextCondition, List<RangeCondition>, pageable)`. It takes the range conditions itself, so example + range + full-text run in one query. The generated service uses `RangeQueryExecutor` when only ranges are present, `FullTextQueryExecutor` as soon as a full-text filter is present. `RangeQueryExecutor` is untouched.
- Future-proofing for follow-ups: `FullTextCondition` is a record that can gain fields (match mode, weights, language) without breaking the executor signature; the Postgres executor already takes the text search configuration as a constructor argument; WHERE-building is separate from SELECT-building so rank ordering can be added.

### Spike result: PostgreSQL (done, works)
- Spring Data JDBC `Criteria` cannot express `@@` (comparators are a closed enum, columns must be mapped properties), and `JdbcAggregateOperations.findAll(Query...)` accepts nothing else. A two-step "ids first" approach was rejected (unbounded IN lists).
- Working approach, `JdbcFullTextQueryExecutor` (postgres module): map example+range criteria to SQL conditions with Spring Data's public `QueryMapper.getMappedObject(...)` (shared with `JdbcRangeQueryExecutor.criteriaOf`), AND with `to_tsvector(cfg, concat_ws(' ', cols...)) @@ websearch_to_tsquery(cfg, :fullTextQuery)` built with the Spring Data SQL DSL (`Comparison`, `SimpleFunction`, bound marker), add sort via `QueryMapper.getMappedSort` and `LIMIT/OFFSET`, render with `SqlRenderer`, execute through `NamedParameterJdbcOperations` with `EntityRowMapper`; total via `PageableExecutionUtils` + `count(1)` query.
- Search text is always a bind parameter; `websearch_to_tsquery` never fails on arbitrary input (tested with SQL-injection-like and unbalanced input). `concat_ws` copes with NULL columns.
- Verified by `JdbcFullTextQueryExecutorTest` (Testcontainers, 7 tests green): any-field match, case insensitivity, AND of terms, `-`/`OR`/quoted phrases, blank query ignored, combined with example and range conditions, paging/sorting/total count. Bean `fullTextQueryExecutor` registered in `PrefabJdbcConfiguration` (config `simple`).
- Known limitation: `SELECT table.*` + `EntityRowMapper` relies on the relation resolver for child entities (N+1 for aggregates with child collections), unlike the join-based select of Spring Data JDBC. Acceptable for now; check with an aggregate that has children when wiring the generator.
- GIN index expression must match the query expression textually: `to_tsvector('simple', concat_ws(' ', col1, col2))`. Note `to_tsvector(regconfig, text)` with a literal config is IMMUTABLE and therefore indexable.
- Not spiked: MongoDB (`TextCriteria` + `Query.addCriteria` on the existing `MongoRangeQueryExecutor` pattern; needs a Mongo `FullTextQueryExecutor`, text index in `MongoIndexWriter`).

### Remaining work
1. Annotation + processor + generated service/query/controller/test client + validation (AC #1-4, #7).
2. Flyway/Liquibase GIN index in `DbMigrationWriter` (AC #5), `MongoIndexWriter` text index (AC #6).
3. `MongoFullTextQueryExecutor` (AC #6).
4. Processor tests, Mongo integration test, example module, Developer Guide (AC #10).
**Suggested breakdown / order**
1. Annotation + processor (grouping, validation, generated params) with processor tests (`FullTextFilterTest`, modelled on `RangeFilterTest`).
2. Core condition type + executor contract.
3. Postgres executor + migration index (Testcontainers test modelled on `JdbcRangeQueryExecutorTest`).
4. Mongo executor + index + integration test.
5. Example module usage + Developer Guide.

**Follow-up stories to create (out of scope)**: fuzzy matching (pg_trgm / Atlas Search), relevance scoring and sort-by-rank, per-filter language/config, prefix/type-ahead search.

- `Filter.Operator.FULL_TEXT` + `Filter.parameter()`; `GetListUtil` collapses FULL_TEXT filters into one `FilterManifest` per parameter, so controller, query, and test-client writers needed no changes.
- `GetListServiceWriter` calls `FullTextQueryExecutor.findAll(example, FullTextCondition, ranges, pageable)`; `GetListPlugin` validates (non-String, >1 parameter, parameter clash).
- Postgres: `JdbcFullTextQueryExecutor` (registered in `PrefabJdbcConfiguration`), GIN index `<table>_<cols>_fts` on `to_tsvector('simple', concat_ws(' ', cols))`; no btree index for full-text-only fields. `SqlStatementParser` got a regex path because JSqlParser cannot parse function-call index expressions when reading back existing migrations.
- MongoDB: `MongoFullTextQueryExecutor` (`TextCriteria`) + generated text index (`default_language none`). Covered by processor tests only; no Mongo integration test was run.
- Example: `Article` in `examples/kafka` with `ArticleFullTextIntegrationTest` (compiles, but could not be run locally because the Kafka Testcontainers image could not be pulled). Postgres behaviour is verified by `JdbcFullTextQueryExecutorTest`.
- Docs: `backlog/docs/annotation-reference.md`.
- Known limitation: the Postgres executor uses `EntityRowMapper`, so child collections of aggregates load through the relation resolver (N+1); untested with aggregates that have children.