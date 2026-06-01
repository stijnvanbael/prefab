---
id: TASK-244
title: Extend @Autocomplete with scan mode and match strategy
status: Done
assignee: []
created_date: '2026-06-01 09:15'
updated_date: '2026-06-01 09:43'
labels: []
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Extend the @Autocomplete annotation to give callers control over two orthogonal dimensions: (1) where in the field value the search term is matched (prefix vs contains) and (2) how the comparison is performed (exact, ignore-case, or fuzzy).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 A new ScanMode enum with PREFIX and CONTAINS values is added to the annotation-processor module with PREFIX as the default
- [x] #2 A new MatchStrategy enum with EXACT, IGNORE_CASE, and FUZZY values is added
- [x] #3 @Autocomplete gains a matchStrategy() attribute defaulting to IGNORE_CASE (preserving current ignoreCase behaviour)
- [x] #4 The existing ignoreCase() attribute is removed from @Autocomplete
- [x] #5 The annotation processor generates queries using LIKE 'term%' for PREFIX and LIKE '%term%' for CONTAINS
- [x] #6 FUZZY strategy applies trigram or Levenshtein-distance-based similarity (algorithm to be decided during implementation)
- [x] #7 Unit tests cover all combinations of ScanMode x MatchStrategy for generated query output
- [x] #8 annotation-reference.md and feature-guides.md are updated to document both new attributes
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
## Technical Analysis

### Current State

`@Autocomplete` (in `core`) has two attributes today:

| Attribute | Type | Default | Effect |
|---|---|---|---|
| `path()` | `String` | `""` | endpoint path override |
| `ignoreCase()` | `boolean` | `false` | wraps column in `LOWER()` / adds `'i'` flag |
| `security()` | `@Security` | `@Security` | endpoint security |

The annotation processor reads `ignoreCase()` in `AutocompleteRepositoryWriter` and emits one of two hard-coded
query shapes — both always use `CONTAINS` semantics (`LIKE '%term%'`). There is no concept of a scan mode
(prefix vs contains) and the match strategy is collapsed into a single boolean.

---

### Proposed Model

Two new enums live in `core/src/main/java/be/appify/prefab/core/annotations/rest/`:

```java
// ScanMode.java
public enum ScanMode {
    /** Match values that start with the query term. Generates LIKE 'term%'. */
    PREFIX,
    /** Match values that contain the query term anywhere. Generates LIKE '%term%'. */
    CONTAINS
}

// MatchStrategy.java
public enum MatchStrategy {
    /** Byte-for-byte equality comparison. */
    EXACT,
    /** Case-insensitive comparison (wraps column in LOWER() for JDBC; adds '$options':'i' for Mongo). */
    IGNORE_CASE,
    /** Fuzzy similarity match (trigram pg_trgm for JDBC; $regex with tolerance for Mongo). */
    FUZZY
}
```

`@Autocomplete` is updated:

```java
ScanMode    scanMode()       default ScanMode.PREFIX;
MatchStrategy matchStrategy() default MatchStrategy.IGNORE_CASE;
// ignoreCase() removed — was false by default, new default is IGNORE_CASE which is a behaviour change;
// existing users who relied on ignoreCase=false must migrate to matchStrategy=EXACT.
```

> **Breaking change note**: the current default of `ignoreCase = false` means most users get case-sensitive
> matching today. Switching the default to `IGNORE_CASE` is a behaviour change. The task AC says to preserve
> current `ignoreCase` behaviour — this means the default must be `MatchStrategy.EXACT`, not `IGNORE_CASE`,
> unless the AC is intentionally relaxing backwards compatibility. **This must be clarified before implementation.**
> Recommendation: default to `MatchStrategy.EXACT` for strict backwards compatibility, or add a migration note.

---

### Files to Change

#### `core` module

| File | Change |
|---|---|
| `core/.../rest/Autocomplete.java` | Remove `ignoreCase()`. Add `scanMode()` and `matchStrategy()`. |
| `core/.../rest/ScanMode.java` | New enum: `PREFIX`, `CONTAINS`. |
| `core/.../rest/MatchStrategy.java` | New enum: `EXACT`, `IGNORE_CASE`, `FUZZY`. |

#### `annotation-processor` module

| File | Change |
|---|---|
| `AutocompleteRepositoryWriter.java` | Replace `boolean ignoreCase` param with `ScanMode` + `MatchStrategy`. Derive JDBC SQL and Mongo pipeline from both dimensions. |
| `AutocompleteRepositoryWriterTest.java` | Expand test matrix: 2 ScanModes × 3 MatchStrategies = 6 cases each for JDBC and Mongo. |

#### Test resources

| File | Change |
|---|---|
| `rest/autocomplete/source/Product.java` | Update `@Autocomplete(ignoreCase = true)` to use new attributes. |

#### Docs

| File | Change |
|---|---|
| `backlog/docs/annotation-reference.md` | Document `scanMode()`, `matchStrategy()`, both enums, defaults, migration note. |
| `backlog/docs/feature-guides.md` | How-to examples for each combination. |

---

### Generated Query Matrix

#### JDBC

| ScanMode | MatchStrategy | Generated WHERE clause |
|---|---|---|
| PREFIX | EXACT | `"col" LIKE CONCAT(:query, '%')` |
| PREFIX | IGNORE_CASE | `LOWER("col") LIKE LOWER(CONCAT(:query, '%'))` |
| PREFIX | FUZZY | `similarity("col", :query) > 0.3 OR "col" ILIKE CONCAT(:query, '%')` *(needs pg_trgm)* |
| CONTAINS | EXACT | `"col" LIKE CONCAT('%', :query, '%')` *(current default)* |
| CONTAINS | IGNORE_CASE | `LOWER("col") LIKE LOWER(CONCAT('%', :query, '%'))` *(current ignoreCase=true)* |
| CONTAINS | FUZZY | `similarity("col", :query) > 0.3` *(needs pg_trgm extension)* |

#### MongoDB

| ScanMode | MatchStrategy | Generated $match stage |
|---|---|---|
| PREFIX | EXACT | `{ '$regex': '^?0' }` |
| PREFIX | IGNORE_CASE | `{ '$regex': '^?0', '$options': 'i' }` |
| PREFIX | FUZZY | `{ '$regex': '^?0', '$options': 'i' }` *(Mongo has no native fuzzy; use anchored regex as best effort)* |
| CONTAINS | EXACT | `{ '$regex': '?0' }` *(current ignoreCase=false)* |
| CONTAINS | IGNORE_CASE | `{ '$regex': '?0', '$options': 'i' }` *(current ignoreCase=true)* |
| CONTAINS | FUZZY | `{ '$regex': '?0', '$options': 'i' }` *(same as IGNORE_CASE; document limitation)* |

---

### FUZZY — Open Risk

JDBC FUZZY requires the `pg_trgm` PostgreSQL extension (`CREATE EXTENSION IF NOT EXISTS pg_trgm`).
The processor cannot install it; a Flyway migration must be added when FUZZY is selected.
The threshold (default `0.3`) could be a future attribute. For now hard-code and document it.

MongoDB has no server-side fuzzy matching. Using an anchored/unanchored regex with `'i'` option is the
closest approximation. Document this limitation clearly in the annotation reference.

**Recommendation**: Treat FUZZY as a separate follow-up task. Implement PREFIX/CONTAINS × EXACT/IGNORE_CASE
(4 combinations) in this task, add a compile-time warning if FUZZY is used, and track full FUZZY support
separately.

---

### Internal API Change

`AutocompleteRepositoryWriter` private methods currently take `boolean ignoreCase`. The refactored signature should be:

```java
static MethodSpec autocompleteJdbcMethod(String fieldName, String tableName, ScanMode scanMode, MatchStrategy matchStrategy)
static MethodSpec autocompleteMongoMethod(String fieldName, ScanMode scanMode, MatchStrategy matchStrategy)
```

The call site in `autocompleteMethods()` reads both annotation attributes and passes them through.

---

### Migration Guide (for existing users)

| Old annotation | New equivalent |
|---|---|
| `@Autocomplete` | `@Autocomplete(scanMode = PREFIX, matchStrategy = EXACT)` |
| `@Autocomplete(ignoreCase = true)` | `@Autocomplete(scanMode = CONTAINS, matchStrategy = IGNORE_CASE)` |
| `@Autocomplete(path = "/x")` | `@Autocomplete(path = "/x")` — no change |

---

### Unknowns to Resolve Before Implementation

1. **Default backwards compatibility**: should the default `matchStrategy` be `EXACT` (safe, no behaviour change)
   or `IGNORE_CASE` (better UX but breaks existing deployments)? Recommend `EXACT`.
2. **FUZZY scope**: include in this task or defer? Recommendation: defer.
3. **pg_trgm migration**: if FUZZY is in scope, who owns the Flyway migration generation?
<!-- SECTION:PLAN:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Implementation

### New types (core module)
- `ScanMode` enum: `PREFIX` | `CONTAINS`
- `MatchStrategy` enum: `EXACT` | `IGNORE_CASE` | `FUZZY`

### Updated `@Autocomplete` annotation
- Removed `boolean ignoreCase()`
- Added `ScanMode scanMode()` defaulting to `ScanMode.PREFIX`
- Added `MatchStrategy matchStrategy()` defaulting to `MatchStrategy.IGNORE_CASE`
- Full Javadoc with migration guide from old `ignoreCase`

### Updated `AutocompleteRepositoryWriter`
- Both static helper methods now take `ScanMode` + `MatchStrategy` instead of `boolean ignoreCase`
- JDBC generates 6 distinct WHERE clause shapes via switch expressions
- MongoDB generates 6 distinct `$match` stage shapes (FUZZY falls back to IGNORE_CASE regex; documented limitation)
- FUZZY on JDBC generates `similarity(col, :query) > 0.3 OR LOWER(col) LIKE LOWER(...)` (requires pg_trgm)

### Tests
- `AutocompleteRepositoryWriterTest`: 18 tests covering all 2 × 3 combinations for both JDBC and Mongo
- `AutocompletePluginTest`: integration assertions updated to match new query shapes (brand defaults to PREFIX + IGNORE_CASE)

### Test resource
- `Product.java` migrated from `ignoreCase = true` to explicit `scanMode = CONTAINS, matchStrategy = IGNORE_CASE`

### Docs
- `annotation-reference.md`: new `@Autocomplete` section added with attribute table, enum reference, query matrix, and migration guide
- `feature-guides.md`: new section 7.17 "Autocomplete Endpoints" with examples for each combination

### Commit
`feat(autocomplete): add ScanMode and MatchStrategy to @Autocomplete, remove ignoreCase` (110dfe70)
<!-- SECTION:FINAL_SUMMARY:END -->
