# Database Migration Lifecycle

**Part of the [Prefab Developer Guide](developer-guide.md)**

This guide explains what Prefab generates, what Flyway executes, and what remains the application's responsibility.
The PostgreSQL and MongoDB migration paths are different; do not apply the behavior of one backend to the other.

## PostgreSQL: generated SQL and Flyway

### Generation, versioning, and execution

When Spring Data JDBC is on the annotation-processor classpath, Prefab derives the desired relational schema from
aggregate roots. Migration generation is enabled by default; `@DbMigration(enabled = false)` opts an aggregate out.
Prefab compares that model with the schema reconstructed by replaying the versioned SQL files it finds on the compile
classpath. It reads `V{n}__*.sql` files in version order, including files named `__manual` or `__baseline`, and
parses their schema statements to build the prior state. It does **not** connect to a database, query
`flyway_schema_history`, or know whether a script has actually been applied. Here, "baseline reconstruction" means
replaying migration SQL into the processor's in-memory schema model; it is not Flyway's `baseline` operation.
Suffixes such as `__baseline` are ordinary filename labels and receive no special treatment.

If there is a schema delta, the annotation processor writes one file for the discovered aggregate changes to
`db/migration/V{next}__generated.sql` in compiler output (normally `target/classes/db/migration/`). The first version
is `V1`; each later generated version is one higher than the highest versioned SQL migration found. No delta means
no generated file. This is compile-time generation, not execution. Package the compiled resources with the
application; Spring Boot's Flyway integration executes pending versioned migrations at application startup.

Generated SQL is a proposal, not a deployment plan. For reviewable, durable migration history, copy the generated
script into `src/main/resources/db/migration/` under a unique versioned name and commit it before deployment. Treat
that source-controlled file as immutable once applied. The generator reconstructs schema state from these files on
subsequent builds, so retain every migration needed to describe the schema. Manual and generated SQL can coexist in
the same Flyway directory; avoid making the same schema change in both.

This reconstruction is only as useful as the SQL parser's understanding of the scripts. Keep schema-changing
manual SQL compatible with the migration parser and compile after adding it. A Flyway-valid statement that the
processor cannot interpret can prevent generation or make the reconstructed schema incomplete; a clean build and
review of the generated delta are essential.

### Reading and reviewing the generated delta

For example, a model with a product name might start as:

```java
@Aggregate
public record Product(@Id Reference<Product> id, @Version long version, String name) {}
```

The initial migration creates the table and columns. Later model edits produce changes like these (identifiers and
types follow the actual fields):

| Model change | Typical generated change |
|---|---|
| Add nullable `String description` | `ALTER TABLE "product" ADD COLUMN "description" VARCHAR (255);` |
| Rename `name` to `displayName` and annotate the new field `@DbRename("name")` | `ALTER TABLE "product" RENAME COLUMN "name" TO "display_name";` |
| Remove a persisted field | `ALTER TABLE` with `DROP COLUMN`; removing an aggregate can produce `DROP TABLE` |
| Change a field's SQL type | `ALTER COLUMN ... TYPE ... USING CAST(...)` |
| Make a nullable field non-null | Add/set the specified default, then `SET NOT NULL`; see the rollout notes below |
| Make a non-null field nullable | `ALTER COLUMN ... DROP NOT NULL` |
| Add/remove an `@Indexed` field index | `CREATE INDEX` / `DROP INDEX` |

`@DbRename` is a migration hint: without it, a renamed field is treated as a removal plus an addition, which can
discard data. The same annotation on an aggregate type signals a table rename. Nested record values are flattened
into column names such as `address_city`; a `@DbRename` on a nested field is resolved against that flattened name.
Lists of nested record values are stored in child tables. Prefab generates the parent-reference foreign key and
index for these tables; inspect the emitted constraint and table names when reviewing a change.

For example:

```java
@Aggregate
public record Order(
        @Id Reference<Order> id,
        @Version long version,
        @NotNull List<OrderLine> lines
) {
    public record OrderLine(@NotNull String productId, int quantity) {}
}
```

The generated child-table migration contains a parent-reference foreign key and an index for that reference:

```sql
CREATE TABLE "order_order_line" (
  "order" VARCHAR (255) NOT NULL REFERENCES "order"("id"),
  "product_id" VARCHAR (255) NOT NULL,
  "quantity" INT NOT NULL,
  "order_key" INT NOT NULL,
  PRIMARY KEY("order", "order_key")
);
CREATE INDEX "order_order_line_order_ix" ON "order_order_line" ("order");
```

For a nested value `Address(String city, String country)` on `Product`, Prefab flattens the fields to
`address_city` and `address_country`. Renaming the nested component with `@DbRename("oldCity") String city`
produces `ALTER TABLE "product" RENAME COLUMN "address_old_city" TO "address_city";`.

Use `@DbDefaultValue` when a new non-null column needs a SQL default or when making an existing nullable column
non-null:

```java
@DbDefaultValue("pending")
@NotNull
String status
```

The value is quoted according to the column type. For a populated table, a generated default is not a substitute for
a domain-specific backfill or a staged rollout. A change to the default on an existing column by itself is not
currently emitted as an `ALTER COLUMN` change; write and review a manual SQL migration for that case. Likewise,
perform data transformations in explicit manual migrations, not in generated schema diffs.

For example, changing an existing nullable `status` field to `@NotNull` with this default generates:

```sql
ALTER TABLE "product"
ALTER COLUMN status SET DEFAULT 'pending',
ALTER COLUMN status SET NOT NULL;
```

Changing a `String price` to `BigDecimal price` generates a type conversion in this form:

```sql
ALTER TABLE "product"
ALTER COLUMN price TYPE DECIMAL (19, 4) USING CAST(price AS DECIMAL (19, 4));
```

Example staged backfill: first add `normalizedName` as nullable and deploy its generated migration, then add
`src/main/resources/db/migration/V3__backfill_normalized_name.sql`:

```sql
UPDATE "product"
SET "normalized_name" = lower("name")
WHERE "normalized_name" IS NULL;
```

After the backfill has completed in the target environment, update the model to make the field non-null and provide
an appropriate `@DbDefaultValue` if required. Compile and review the next generated migration before deploying it.
Choose version numbers that fit the existing history; the example `V3` assumes `V2` is the preceding migration.

### Deployment, history, and recovery

Flyway records applied versions and checksums in its schema history table. At startup it validates the files against
that history and runs pending migrations; it does not make an unsafe schema change safe. Editing a migration that
has already run causes checksum validation to fail and can conceal a mismatch between the recorded history and the
database. Do not edit or replace an applied migration. Add a new, reviewed forward migration instead.

Review every generated file before release, especially `DROP COLUMN`, `DROP TABLE`, type casts, `SET NOT NULL`, and
index replacement. Confirm data preservation, lock/runtime impact, backfill order, and compatibility with versions
of the application that may be running concurrently. Prefab does not provide zero-downtime rollout orchestration,
automatic data backfills, or production schema recovery. If production migration validation fails, stop and assess
the live schema and Flyway history with the database operator; do not delete the production schema or use the test
reset behavior described below as a repair.

### Test-only checksum reset

`@IntegrationTest` imports a test Flyway strategy. When the test property
`prefab.flyway.auto-clean-on-mismatch=true` is enabled, it checks the last applied migration; if its checksum does
not match, the strategy calls `Flyway.clean()` and then migrates again. This is a destructive test convenience for
disposable test databases, not a production recovery procedure. Enable Flyway clean only for isolated test
environments and never set this up against production data. Without that property, a checksum mismatch fails
normally and must be investigated.

## MongoDB: generated rename scripts

With Spring Data MongoDB on the processor classpath, Prefab can generate JavaScript migration files under
`mongo/migration/V{n}__generated.js` in compiler output (normally `target/classes/mongo/migration/`). Unlike the
PostgreSQL path, it does not diff a relational schema. The Mongo generator recognizes `@DbRename` on a collection
type or a field, including nested fields, and emits collection/field renames:

```java
@Aggregate
@DbMigration
@DbRename("LegacyProduct")
public record Product(
        @Id Reference<Product> id,
        @DbRename("oldName") String name,
        Price price
) {
    public record Price(@DbRename("oldAmount") BigDecimal amount, String currency) {}
}
```

The generated operations have this form:

```javascript
db.getCollection("legacyProduct").renameCollection("product");
db.getCollection("product").updateMany({}, { $rename: { "oldName": "name" } });
db.getCollection("product").updateMany({}, { $rename: { "price.oldAmount": "price.amount" } });
```

The generator scans prior generated `V{n}__generated.js` files and avoids emitting rename operations it recognizes
there. This is not a MongoDB migration history or checksum system: Prefab does not run these scripts at startup,
track their application in MongoDB, or generate general-purpose scripts for field additions, removals, type changes,
defaults, indexes, or data backfills. Plan, version, review, execute, and record MongoDB migrations with the
application's chosen operational process. Do not assume Flyway's SQL execution or checksum behavior applies to
MongoDB.

MongoDB integration tests have a separate test helper that removes all documents before each test instance is prepared;
it keeps the collections, so the indexes created at startup survive.
That reset is test-only and is unrelated to generated MongoDB migration execution.

See also [Generated Artefacts: Database migrations](generated-artefacts.md#66-database-migration-scripts) and
[Troubleshooting](troubleshooting.md#generated-migration-script-conflicts-with-existing-schema).
