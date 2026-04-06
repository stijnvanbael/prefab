package be.appify.prefab.processor.mongodb;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;

class MongoMigrationWriterTest {

    private final MongoMigrationWriter writer = new MongoMigrationWriter();

    // ──────────────────────────────────────────────────────────────
    // Script format tests
    // ──────────────────────────────────────────────────────────────

    @Test
    void renameCollectionScriptFormat() {
        var change = new MongoChange.RenameCollection("oldProduct", "product");
        assertThat(change.toScript())
                .isEqualTo("db.getCollection(\"oldProduct\").renameCollection(\"product\");\n");
    }

    @Test
    void renameFieldScriptFormat() {
        var change = new MongoChange.RenameField("product", "firstName", "givenName");
        assertThat(change.toScript())
                .isEqualTo("db.getCollection(\"product\").updateMany({}, { $rename: { \"firstName\": \"givenName\" } });\n");
    }

    @Test
    void renameNestedFieldScriptFormat() {
        var change = new MongoChange.RenameField("product", "price.oldAmount", "price.amount");
        assertThat(change.toScript())
                .isEqualTo("db.getCollection(\"product\").updateMany({}, { $rename: { \"price.oldAmount\": \"price.amount\" } });\n");
    }

    // ──────────────────────────────────────────────────────────────
    // Collection name derivation
    // ──────────────────────────────────────────────────────────────

    @Test
    void mongoCollectionNameFromSimpleName() {
        assertThat(MongoMigrationWriter.mongoCollectionNameFrom("Product")).isEqualTo("product");
    }

    @Test
    void mongoCollectionNameFromCamelCaseName() {
        assertThat(MongoMigrationWriter.mongoCollectionNameFrom("CategoryStats")).isEqualTo("categoryStats");
    }

    @Test
    void mongoCollectionNameFromAlreadyLowerCase() {
        assertThat(MongoMigrationWriter.mongoCollectionNameFrom("product")).isEqualTo("product");
    }

    // ──────────────────────────────────────────────────────────────
    // Parsing existing migration files
    // ──────────────────────────────────────────────────────────────

    @Test
    void parseCollectionRenameFromExistingMigration() {
        var content = "db.getCollection(\"oldProduct\").renameCollection(\"product\");\n";
        var changes = MongoMigrationWriter.parseChanges(content);
        assertThat(changes).containsExactly(new MongoChange.RenameCollection("oldProduct", "product"));
    }

    @Test
    void parseFieldRenameFromExistingMigration() {
        var content = "db.getCollection(\"product\").updateMany({}, { $rename: { \"firstName\": \"givenName\" } });\n";
        var changes = MongoMigrationWriter.parseChanges(content);
        assertThat(changes).containsExactly(new MongoChange.RenameField("product", "firstName", "givenName"));
    }

    @Test
    void parseMultipleChangesFromExistingMigration() {
        var content = """
                db.getCollection("oldProduct").renameCollection("product");
                db.getCollection("product").updateMany({}, { $rename: { "firstName": "givenName" } });
                db.getCollection("product").updateMany({}, { $rename: { "price.oldAmount": "price.amount" } });
                """;
        var changes = MongoMigrationWriter.parseChanges(content);
        assertThat(changes).containsExactlyElementsIn(List.of(
                new MongoChange.RenameCollection("oldProduct", "product"),
                new MongoChange.RenameField("product", "firstName", "givenName"),
                new MongoChange.RenameField("product", "price.oldAmount", "price.amount")
        ));
    }

    @Test
    void parseEmptyMigrationReturnsNoChanges() {
        assertThat(MongoMigrationWriter.parseChanges("")).isEmpty();
    }

    // ──────────────────────────────────────────────────────────────
    // New-change detection
    // ──────────────────────────────────────────────────────────────

    @Test
    void newChangesExcludesAlreadyApplied() {
        var applied = Set.<MongoChange>of(
                new MongoChange.RenameCollection("oldProduct", "product")
        );
        var desired = List.<MongoChange>of(
                new MongoChange.RenameCollection("oldProduct", "product"),
                new MongoChange.RenameField("product", "firstName", "givenName")
        );

        var newChanges = desired.stream()
                .filter(c -> !applied.contains(c))
                .toList();

        assertThat(newChanges).containsExactly(
                new MongoChange.RenameField("product", "firstName", "givenName")
        );
    }

    @Test
    void allChangesNewWhenNothingApplied() {
        var applied = Set.<MongoChange>of();
        var desired = List.<MongoChange>of(
                new MongoChange.RenameCollection("oldProduct", "product"),
                new MongoChange.RenameField("product", "firstName", "givenName")
        );

        var newChanges = desired.stream()
                .filter(c -> !applied.contains(c))
                .toList();

        assertThat(newChanges).hasSize(2);
    }

    @Test
    void noNewMigrationWhenAllAlreadyApplied() {
        var applied = Set.<MongoChange>of(
                new MongoChange.RenameCollection("oldProduct", "product"),
                new MongoChange.RenameField("product", "firstName", "givenName")
        );
        var desired = List.<MongoChange>of(
                new MongoChange.RenameCollection("oldProduct", "product"),
                new MongoChange.RenameField("product", "firstName", "givenName")
        );

        var newChanges = desired.stream()
                .filter(c -> !applied.contains(c))
                .toList();

        assertThat(newChanges).isEmpty();
    }

    // ──────────────────────────────────────────────────────────────
    // Round-trip: generate script and re-parse it
    // ──────────────────────────────────────────────────────────────

    @Test
    void roundTripCollectionRename() {
        var change = new MongoChange.RenameCollection("oldProduct", "product");
        var parsed = MongoMigrationWriter.parseChanges(change.toScript());
        assertThat(parsed).containsExactly(change);
    }

    @Test
    void roundTripFieldRename() {
        var change = new MongoChange.RenameField("product", "firstName", "givenName");
        var parsed = MongoMigrationWriter.parseChanges(change.toScript());
        assertThat(parsed).containsExactly(change);
    }

    @Test
    void roundTripNestedFieldRename() {
        var change = new MongoChange.RenameField("product", "price.oldAmount", "price.amount");
        var parsed = MongoMigrationWriter.parseChanges(change.toScript());
        assertThat(parsed).containsExactly(change);
    }
}
