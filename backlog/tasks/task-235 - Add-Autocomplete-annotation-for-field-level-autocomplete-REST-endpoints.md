---
id: TASK-235
title: Add @Autocomplete annotation for field-level autocomplete REST endpoints
status: In Progress
assignee: []
created_date: '2026-05-27 13:15'
updated_date: '2026-05-27 13:21'
labels:
  - ✨feature
  - rest
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Introduce an `@Autocomplete` field-level annotation that generates a dedicated REST endpoint returning distinct matching values for the annotated field — not entire aggregates. One endpoint is generated per annotated field. The URL is configurable via the `path()` attribute.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 @Autocomplete annotation defined in prefab-core with path(), ignoreCase(), and security() attributes
- [ ] #2 AutocompletePlugin registered in META-INF/services
- [ ] #3 Controller generates GET endpoint per field returning ResponseEntity<List<String>>
- [ ] #4 Service generates autocomplete method using Spring Data Example API
- [ ] #5 Repository adds findAll(Example, Pageable) when not already present from @Filter
- [ ] #6 TestClient generates corresponding autocomplete method
- [ ] #7 Unit tests verify generated controller, service, and repository code
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Create `@Autocomplete` annotation in `core` (path, ignoreCase, security attributes)
2. Create `annotation-processor/.../rest/autocomplete/` package
3. `AutocompleteControllerWriter` — one GET method per annotated field, default path `/{kebab-field-name}/autocomplete`
4. `AutocompleteServiceWriter` — one service method per field, delegates to repository, uses `PageRequest.of(0, 10)`
5. `AutocompleteRepositoryWriter` — detects JDBC vs MongoDB; generates `@Query` (JDBC SQL) or `@Aggregation` (MongoDB) method; respects `ignoreCase`
6. `AutocompleteTestClientWriter` — test helper method matching the controller endpoint
7. `AutocompletePlugin` — wires all four writers, implements `PrefabPlugin`
8. Register `AutocompletePlugin` in `META-INF/services`
9. Add test source file `rest/autocomplete/source/Product.java` with `@Autocomplete` field
10. Write `AutocompletePluginTest` asserting controller, service, and repository generated code
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
## Analysis

### Why the Example-based approach (findAll + in-memory distinct) is wrong

`GetListRepositoryWriter` generates `findAll(Example<T>, Pageable)` which returns **full aggregate
objects**. Using this for autocomplete would:

1. **Fetch every column** of every matching row from the database.
2. Require **in-memory deduplication** of field values — O(n) on the result set.
3. Silently scale badly: a 100K-row table with 50 matches returns 50 fully hydrated aggregates just
   to extract one String per row.

The requirement is a **single-column, deduplicated, DB-side query** that returns only the values
relevant to the autocomplete widget.

---

### Spring Data has no cross-database "find distinct field values" abstraction

Spring Data Commons (`CrudRepository`, `PagingAndSortingRepository`) has no method for selecting
distinct scalar field values. The `QueryByExampleExecutor.findAll(Example, Pageable)` still returns
full entities. There is no portable `findDistinctFieldValues(...)` in the commons layer.

This forces **database-specific query generation**, exactly as `MongoIndexPlugin` and
`DbMigrationPlugin` already do for indexes and migrations.

---

### Database detection — established pattern

| DB | Detection class | Detection flag constant |
|----|----------------|------------------------|
| Spring Data JDBC (PostgreSQL) | `org.springframework.data.relational.core.mapping.Table` | `JDBC_INCLUDED` (see `DbMigrationPlugin`) |
| Spring Data MongoDB | `org.springframework.data.mongodb.core.MongoTemplate` | `MONGO_INCLUDED` (see `MongoIndexPlugin`) |

Both flags are set once at class-load time and guard the generation. The same pattern will be used
in `AutocompleteRepositoryWriter`.

---

### Generated repository method — Spring Data JDBC (PostgreSQL)

Spring Data JDBC `@Query` uses **raw SQL** (not JPQL). Column/table names are derived with
`CaseUtil.toSnakeCase()`, matching the naming convention used by `DbMigrationWriter`.

```java
// ignoreCase = true
@Query("SELECT DISTINCT \"column_name\" FROM \"table_name\" " +
       "WHERE LOWER(\"column_name\") LIKE LOWER(CONCAT('%', :query, '%')) " +
       "ORDER BY \"column_name\"")
List<String> autocomplete{FieldName}(@Param("query") String query, Pageable pageable);

// ignoreCase = false
@Query("SELECT DISTINCT \"column_name\" FROM \"table_name\" " +
       "WHERE \"column_name\" LIKE CONCAT('%', :query, '%') " +
       "ORDER BY \"column_name\"")
List<String> autocomplete{FieldName}(@Param("query") String query, Pageable pageable);
```

`@Param` is `org.springframework.data.repository.query.Param`.  
`@Query` is `org.springframework.data.jdbc.repository.query.Query`.

---

### Generated repository method — Spring Data MongoDB

MongoDB has no `@Query` scalar projection that returns `List<String>` cleanly. The correct tool is
Spring Data MongoDB's `@Aggregation` pipeline, which runs natively in MongoDB and returns scalar
values when the final `$project` exposes a single field.

```java
// ignoreCase = true
@Aggregation(pipeline = {
    "{ '$match': { 'fieldName': { '$regex': '?0', '$options': 'i' } } }",
    "{ '$group': { '_id': '$fieldName' } }",
    "{ '$sort': { '_id': 1 } }"
})
List<String> autocomplete{FieldName}(String query, Pageable pageable);

// ignoreCase = false
@Aggregation(pipeline = {
    "{ '$match': { 'fieldName': { '$regex': '?0' } } }",
    "{ '$group': { '_id': '$fieldName' } }",
    "{ '$sort': { '_id': 1 } }"
})
List<String> autocomplete{FieldName}(String query, Pageable pageable);
```

`@Aggregation` is `org.springframework.data.mongodb.repository.Aggregation`.  
Field name in MongoDB documents is the **Java camelCase field name** (no snake_case conversion).

---

### Service method

With a proper repository method, the service is trivial — no mapping, no deduplication:

```java
public List<String> autocomplete{FieldName}(String query) {
    log.debug("Autocompleting {} by {}", Product.class.getSimpleName(), "{fieldName}");
    return {aggregate}Repository.autocomplete{FieldName}(query, PageRequest.of(0, 10));
}
```

A default page size of **10** is used. A future `limit()` attribute on `@Autocomplete` can
expose this as a configuration option (out of scope here).

---

### Controller method (one per annotated field)

```java
@GetMapping("/{field-name}/autocomplete")   // or @Autocomplete(path = "/custom")
@PreAuthorize("isAuthenticated()")
@Operation(summary = "Autocomplete Product name")
public ResponseEntity<List<String>> autocompleteByName(@RequestParam String query) {
    return ResponseEntity.ok(service.autocompleteByName(query));
}
```

Default URL: `/{kebab-field-name}/autocomplete` relative to the aggregate base path.

---

### Impact on GetListRepositoryWriter

`GetListRepositoryWriter` generates `findAll(Example<T>, Pageable)` only when `@Filter`
annotations are present. `AutocompleteRepositoryWriter` generates a completely different,
field-specific method — there is **no clash** and no need to change `GetListRepositoryWriter`.
<!-- SECTION:NOTES:END -->
