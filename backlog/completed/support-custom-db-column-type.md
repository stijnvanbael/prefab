---
id: TBD
title: "Add @DbColumn annotation to declare a custom SQL column type for aggregate fields"
status: "To Do"
priority: "High"
labels: ["feature", "annotation-processor", "requested-by:maestro"]
---

## Background / Problem Statement

Prefab's annotation processor validates every aggregate field type and maps it to a
known SQL column type. If a field type is not in the supported set (e.g. `float[]`,
`PGobject`, custom value types), the processor throws an `IllegalArgumentException`
at compile time — even when `@DbMigration(enabled = false)` is declared on the
aggregate.

This prevents using **custom PostgreSQL types** (e.g. `pgvector`'s `vector(N)`,
PostGIS geometry, hstore) as native Java field types.  The current workarounds are
all painful:

| Workaround | Problem |
|---|---|
| `@DbDocument List<Float>` | Prefab serialises to JSONB *before* any Spring Data JDBC converter runs — the converter is never called |
| `List<Float>` without `@DbDocument` | Compiler accepts it, but a `Converter<List<Float>, PGobject>` is unsafe due to Java's type erasure: it matches **any** `List` at runtime, breaking other `List<Reference<T>>` fields |
| `float[]` / `Float[]` | Rejected outright by the processor: `IllegalArgumentException: Unsupported type: float[]` |

The Maestro project hit all three dead ends implementing the `MemoryEntry` aggregate
(Maestro M-012), which needs a `vector(1536)` column for pgvector semantic
similarity search.

## Proposed Solution

Add a new field-level annotation `@DbColumn` that:

1. **Tells the annotation processor to treat the field as an opaque, custom-typed
   column** — skipping Prefab's built-in type validation for that field.
2. **Specifies the exact SQL column DDL type** used when generating the Flyway
   migration (used when `@DbMigration` is enabled; ignored when disabled).
3. **Optionally references a converter class** that is automatically registered as
   a Spring Data JDBC `@WritingConverter` / `@ReadingConverter` pair.

```java
@Target(ElementType.RECORD_COMPONENT)
@Retention(RetentionPolicy.RUNTIME)
public @interface DbColumn {

    /**
     * The exact SQL column type to use in the generated Flyway migration DDL.
     * Example: {@code "vector(1536)"}, {@code "geometry(Point,4326)"}, {@code "hstore"}.
     * Required when {@code @DbMigration} is enabled on the enclosing aggregate;
     * ignored (but still useful for documentation) when disabled.
     */
    String type();

    /**
     * Optional converter class that converts between the Java field type and the
     * JDBC representation.  The class must implement Spring's {@code Converter<F, T>}
     * where {@code F} is the field's Java type.  Prefab registers it automatically
     * as a {@code JdbcCustomConversions} contributor so the user does not need to
     * declare it as a {@code @Component} or reference it from a configuration class.
     *
     * <p>Leave as {@code void.class} (the default) if you prefer to register the
     * converter through the standard Prefab {@code PolymorphicReadingConverter}
     * mechanism.
     */
    Class<?> converter() default void.class;
}
```

### Example usage (Maestro `MemoryEntry`)

```java
@Aggregate
@GetById
@GetList
@DbMigration(enabled = false)   // manual migration for now; @DbColumn makes this optional in future
public record MemoryEntry(
        @Id Reference<MemoryEntry> id,
        @Version long version,
        Reference<ConversationSession> sessionId,
        @Text String content,
        @DbColumn(type = "vector(1536)", converter = FloatArrayToVectorConverter.class)
        float[] embedding,
        MemoryType memoryType,
        AuditInfo audit
) { ... }
```

With `@DbColumn`:
- `float[]` is accepted by the annotation processor without error ✓
- The generated DDL (if enabled) contains `vector(1536)` for the column ✓
- `FloatArrayToVectorConverter` (`Converter<float[], PGobject>`) is registered
  automatically — no type-erasure conflict because `float[]` is an array, not a
  generic type ✓

### Why not just expand the built-in type set?

Adding `float[]` natively would require Prefab to know about every possible database
extension type.  `@DbColumn` is the general-purpose escape hatch that covers
pgvector, PostGIS, hstore, and any future custom type without touching core again.

## Acceptance Criteria

- [ ] `@DbColumn` annotation added to `prefab-core` (field-level, runtime retention)
- [ ] Annotation processor: when `@DbColumn` is present on a record component,
  skip the built-in type validation for that component (no `IllegalArgumentException`)
- [ ] Annotation processor: when `@DbMigration` is enabled on the aggregate,
  emit `@DbColumn.type()` as the SQL column type in the generated migration script
- [ ] Annotation processor: when `@DbColumn.converter()` is specified, register
  the referenced class as a `JdbcCustomConversions` contributor at application startup
  (via auto-configuration or bean post-processing)
- [ ] Annotation processor compilation error if `@DbColumn.type()` is blank
- [ ] `@DbColumn` works correctly on `float[]`, `Float[]`, `byte[]`, and user-defined
  record/class types
- [ ] Integration test with `pgvector`: `@DbColumn(type="vector(1536)")` on a `float[]`
  field writes and reads the vector correctly via the supplied converter
- [ ] Prefab developer guide updated with `@DbColumn` reference and a pgvector
  cookbook example

## Impact on Maestro

Once `@DbColumn` is available in Prefab, Maestro's `MemoryEntry` aggregate can be
simplified:

- Remove `ListOfFloatToVectorConverter` — the type-erasure workaround is no longer
  needed; `float[]` is used directly
- Remove `VectorToListOfFloatConverter` — replaced by the converter referenced in
  `@DbColumn`
- Remove the `@DbMigration(enabled = false)` override (optional — the generated
  migration can now include `vector(1536)`)
- Remove the hand-written `V2__memory_entry.sql` (optional — Prefab generates it)
- Keep `FloatArrayToVectorConverter` as the single, type-safe converter

This effort is tracked in Maestro task M-012 and the full pgvector feature roadmap
in `prefab-vector-module.md`.

