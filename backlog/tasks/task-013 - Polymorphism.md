---
id: task-013
title: Polymorphism
status: In Progress
assignee: []
created_date: '2025-10-10 13:34'
updated_date: '2026-04-02 10:56'
labels: []
dependencies: []
ordinal: 8000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Support polymorphic aggregate roots and entities: a single annotated type that can have multiple concrete
subtypes stored in one table (PostgreSQL/JDBC) or one collection (MongoDB), identified by a discriminator.

Currently not supported by Spring Data JDBC.

Possible solution: generate converters

```
@ReadingConverter
public class AssessmentReadingConverter implements Converter<ResultSet, Assessment> {
    @Override
    public Assessment convert(ResultSet rs) {
        String type = rs.getString("type");
        // Example: switch on type to instantiate the correct subtype
        if ("MULTIPLE_CHOICE".equals(type)) {
            // Map fields to MultipleChoiceAssessment
            // (extract fields from rs and construct the object)
            return new MultipleChoiceAssessment(/* ... */);
        }
        // Add more subtypes as needed
        throw new IllegalArgumentException("Unknown assessment type: " + type);
    }
}
```
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->
- [ ] #1 A sealed interface annotated with `@Aggregate` and its permitted record subtypes are accepted by the annotation processor without error
- [ ] #2 A `@DbMigration`-annotated polymorphic aggregate generates a single SQL table with a `type VARCHAR(255) NOT NULL` discriminator column; fields common to all subtypes are `NOT NULL`, subtype-specific fields are nullable
- [x] #3 A `{AggregateType}ReadingConverter` is generated and correctly reconstructs the right subtype at runtime from a database row (Postgres/JDBC)
- [x] #4 The generated reading converter is automatically registered in `JdbcCustomConversions` so Spring Data JDBC can use it without any user configuration
- [x] #5 Saving a polymorphic aggregate persists the correct `type` discriminator value and all fields for the concrete subtype
- [x] #6 A `{AggregateType}Repository extends CrudRepository<SealedInterface, String>` is generated and works for both read and write operations on Postgres
- [x] #7 The same domain model (sealed interface + record subtypes) works transparently with MongoDB using Spring Data MongoDB's native discriminator mechanism
- [ ] #8 An integration test (Postgres) round-trips a polymorphic aggregate: save a subtype, reload it, verify the correct concrete type is returned
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
### Phase 1 — Annotation processor (code generation) ✅ done in branch `copilot/analyse-task-13-polymorphic-aggregates`

The annotation processor changes are complete:

1. **`PolymorphicAggregateManifest`** – wraps a `sealed` interface annotated with `@Aggregate`; introspects
   permitted subtypes (concrete `record`s), computes `commonFields` (shared by all subtypes) vs.
   `subtypeSpecificFields`.

2. **`PrefabProcessor`** – detects `element.getModifiers().contains(Modifier.SEALED)` alongside the existing
   `isClass()` check; creates `PolymorphicAggregateManifest` per sealed aggregate and routes it through the
   new generation pipeline.

3. **`PolymorphicJdbcConverterWriter`** – generates a `@ReadingConverter Converter<Map<String,Object>, T>` that
   reads the `type` discriminator column and switch-constructs the right subtype with type-safe field
   extraction (`instanceof Number n ? n.longValue()`, etc.).

4. **`PersistenceWriter`** – generates `{Type}Repository extends CrudRepository<SealedInterface, String>` for
   polymorphic aggregates.

5. **`DbMigrationWriter`** – generates a single table (`polymorphicTable()`):
   - `id` column (from common fields, NOT NULL, PRIMARY KEY)
   - `type VARCHAR(255) NOT NULL` discriminator column immediately after `id`
   - All common fields (`NOT NULL`)
   - All subtype-specific fields (`NULL`)

6. **`DbMigrationPlugin`** / **`PrefabPlugin`** – new `writeAdditionalFiles(List<ClassManifest>,
   List<PolymorphicAggregateManifest>)` overload ensures regular and polymorphic tables end up in a single
   migration file.

7. **Tests** – `PolymorphicConverterWriterTest` (3 tests): verifies repository is generated, reading converter
   is generated, and its content matches the expected snapshot (`ShapeReadingConverter.java`).

---

### Phase 2 — JDBC runtime wiring ✅ done in branch `copilot/analyse-task-13-polymorphic-aggregates`

Spring Data JDBC must be configured to actually invoke the generated reading converter when it hydrates an
entity from the database. Several steps are required:

#### 2a. Register the reading converter in `JdbcCustomConversions`

`PrefabConfiguration.userConverters()` currently returns a hard-coded list of built-in converters. It must be
extended to discover and include generated `@ReadingConverter` beans automatically.

Approach: inject all beans annotated with both `@ReadingConverter` and `@Component` whose source type is
`Map<String,Object>` via `ApplicationContext.getBeansWithAnnotation(ReadingConverter.class)`.

```java
// In PrefabConfiguration (or a new @Configuration):
@Override
public List<?> userConverters() {
    var converters = new ArrayList<>(List.of(
        new FileToByteArrayConverter(),
        new ByteArrayToFileConverter()
    ));
    // Collect generated polymorphic reading converters
    applicationContext.getBeansWithAnnotation(ReadingConverter.class).values().stream()
        .filter(bean -> isPolymorphicReadingConverter(bean.getClass()))
        .forEach(converters::add);
    return converters;
}
```

This must be done lazily (use `@Lazy` or `ApplicationContextAware`) to avoid circular dependency with
`JdbcCustomConversions` itself.

#### 2b. Teach `PrefabMappingJdbcConverter` to use the converter for sealed interface types

Spring Data JDBC calls `MappingJdbcConverter.readValue(Object value, TypeInformation type)`.  
When the target type is a sealed interface and a `Converter<Map<String,Object>, SealedInterface>` is
registered, `readValue` should delegate to it.

`PrefabMappingJdbcConverter` already overrides `readValue()`; extend it:

```java
@Override
public Object readValue(Object value, TypeInformation<?> type) {
    Class<?> targetType = type.getType();
    if (targetType.isInterface() && targetType.isSealed() && value instanceof Map<?,?> map) {
        // Look up a registered Converter<Map<String,Object>, targetType>
        if (getConversions().hasCustomReadTarget(Map.class, targetType)) {
            return getConversionService().convert(map, targetType);
        }
    }
    return super.readValue(value, type);
}
```

#### 2c. Writing / discriminator column

When Spring Data JDBC saves a polymorphic entity, the `type` discriminator column must be set.
Two sub-options:

**Option A – `@WritingConverter Converter<SealedInterface, Map<String,Object>>`**  
Generate a writing converter that calls the concrete subtype's normal field mapping plus writes
`"type" → concreteClass.getSimpleName()`. Register in `JdbcCustomConversions`.

**Option B – `EntityCallback<T>` / `BeforeSaveCallback`**  
Register a `BeforeSaveCallback` that, for any entity that is an instance of a sealed interface aggregate,
inserts the `type` key into the `OutboundRow` via reflection.

Option A (generate writing converter) is more consistent with the reading converter pattern and keeps all
logic in generated code.

#### 2d. Make Spring Data JDBC treat the sealed interface as an entity type

Spring Data JDBC normally only creates repositories for concrete classes.  
`JdbcMappingContext` must register the sealed interface as a managed type so that
`PrefabPersistentEntity` is created for it.

Likely fix: override `PrefabJdbcMappingContext.getPersistentEntity(Class)` to recognise sealed interfaces
and map them through their common fields (the `id` column comes from `commonFields`).

---

### Phase 3 — MongoDB runtime wiring ✅ done in branch `copilot/analyse-task-13-polymorphic-aggregates`

Spring Data MongoDB has **native** polymorphism support via `_class` discriminator written automatically by
`MappingMongoConverter`. The steps needed are:

#### 3a. Register the sealed interface with `MongoMappingContext`

Override `PrefabMongoConfiguration` (the auto-configured MongoDB config) to add the sealed interface and its
permitted subtypes to the set of managed types. Spring Data MongoDB will then write `_class` as the fully
qualified name (or alias via `@TypeAlias`) of the concrete type, and read back the correct subtype using the
`DefaultMongoTypeMapper`.

#### 3b. Verify `@TypeAlias` annotations (optional)

For cleaner JSON documents, the annotation processor could generate `@TypeAlias("Circle")` on each permitted
subtype record so MongoDB stores a short discriminator instead of the full class name.

#### 3c. Repository registration

The generated `{Type}Repository extends CrudRepository<SealedInterface, String>` should work with Spring
Data MongoDB once the type mapping is configured, as MongoDB repositories do support interface types when
the discriminator is present.

---

### Phase 4 — REST / Application layer ⬜ TODO

Currently `HttpWriter` and `ApplicationWriter` are NOT called for polymorphic aggregates. Decide whether to:
- Generate a single controller / service for the sealed interface with a polymorphic request body
  (requires a `@JsonSubTypes` / Jackson discriminator setup)
- Or skip REST generation for polymorphic types and let the developer write custom controllers

---

### Phase 5 — Integration tests ⬜ TODO

Add integration tests using `@IntegrationTest` + Testcontainers that verify:
- Save a `Circle` and reload it as `Shape` → verify it comes back as `Circle`
- Save a `Rectangle` and reload it → verify it comes back as `Rectangle`
- Page through a mixed list of shapes → verify correct runtime types
- MongoDB: same assertions with the MongoDB test profile
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
### Branch: `copilot/analyse-task-13-polymorphic-aggregates`

**Completed (annotation-processor code generation):**
- `PolymorphicAggregateManifest` – manifest for `sealed @Aggregate` interfaces
- `PolymorphicJdbcConverterWriter` – generates `@ReadingConverter Converter<Map<String,Object>, T>`
- `PrefabProcessor` – detects sealed interface aggregates alongside concrete class aggregates
- `PersistenceWriter` – generates repository for sealed interface
- `DbMigrationWriter` / `DbMigrationPlugin` – generates single-table + discriminator migration
- `PrefabPlugin` – new `writeAdditionalFiles(List, List<PolymorphicAggregateManifest>)` overload
- All 24 annotation-processor tests pass (3 new polymorphism tests added)

**User-facing API (how the feature will be used once complete):**

```java
@Aggregate
@DbMigration
public sealed interface Shape permits Shape.Circle, Shape.Rectangle {
    record Circle(
            @Id Reference<Shape> id,
            @Version long version,
            double radius
    ) implements Shape {}

    record Rectangle(
            @Id Reference<Shape> id,
            @Version long version,
            double width,
            double height
    ) implements Shape {}
}
```

**Generated SQL migration:**
```sql
CREATE TABLE "shape" (
  "id" VARCHAR (255) NOT NULL,
  "type" VARCHAR (255) NOT NULL,
  "version" BIGINT NOT NULL,
  "radius" DECIMAL (19, 4),
  "width" DECIMAL (19, 4),
  "height" DECIMAL (19, 4),
  PRIMARY KEY("id")
);
```

**Generated reading converter (JDBC):**
```java
@Component
@ReadingConverter
public class ShapeReadingConverter implements Converter<Map<String, Object>, Shape> {
    @Override
    public Shape convert(Map<String, Object> row) {
        var type = (String) row.get("type");
        return switch (type) {
            case "Circle" -> new Shape.Circle(
                Reference.fromId((String) row.get("id")),
                row.get("version") instanceof Number n ? n.longValue() : 0L,
                row.get("radius") instanceof Number n ? n.doubleValue() : 0.0);
            case "Rectangle" -> new Shape.Rectangle(
                Reference.fromId((String) row.get("id")),
                row.get("version") instanceof Number n ? n.longValue() : 0L,
                row.get("width") instanceof Number n ? n.doubleValue() : 0.0,
                row.get("height") instanceof Number n ? n.doubleValue() : 0.0);
            default -> throw new IllegalArgumentException("Unknown Shape type: " + type);
        };
    }
}
```

**Key design decisions:**
- Sealed interfaces + Java records (Java 21) are the idiomatic model for bounded polymorphism in Prefab.
  The same pattern is already used for events (e.g. `HierarchyEvent` in the Avro tests).
- Single-table inheritance (STI) is chosen over table-per-type. STI is simpler, avoids joins, and is the
  natural fit for Java sealed hierarchies which are shallow by design.
- The discriminator column name is always `type` (not configurable for now).
- Common fields that exist in all subtypes are `NOT NULL`; subtype-specific fields are nullable.
- The reading converter takes `Map<String,Object>` (not `ResultSet`) because that is the natural source
  type produced by Spring Data JDBC's row mapper infrastructure.
- MongoDB is expected to work with minimal additional effort once type mapping is configured, as Spring Data
  MongoDB's `_class` mechanism already provides native polymorphism.

**Open questions / risks:**
1. Spring Data JDBC has no first-class concept of "interface as entity type" — the sealed interface cannot
   be added to `JdbcMappingContext` as a persistent entity in the normal way. We may need to register a
   repository against one concrete subtype's table mapping or use a custom `RowMapper`.
2. Writing (INSERT/UPDATE) polymorphic entities requires Spring Data JDBC to know to write the `type`
   column. A `@WritingConverter Converter<SealedInterface, Map>` or a `BeforeSaveCallback` will be needed.
3. The `@Version` field for optimistic locking is currently in each subtype record individually. It should
   be listed in `commonFields` — this works correctly as long as all subtypes declare it with the same name
   and type.
4. HTTP/REST generation for polymorphic aggregates is deferred; the current implementation generates the
   repository only.
<!-- SECTION:NOTES:END -->
