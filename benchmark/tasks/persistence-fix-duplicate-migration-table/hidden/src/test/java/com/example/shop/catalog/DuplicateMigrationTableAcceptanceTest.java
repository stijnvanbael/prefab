package com.example.shop.catalog;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateMigrationTableAcceptanceTest {
    @Test
    void duplicateCreateIsGuardedAndDoesNotGenerateAnotherMigration() throws Exception {
        var migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V2__restore_product_manual.sql"));
        assertThat(migration.toUpperCase(java.util.Locale.ROOT))
                .contains("CREATE TABLE IF NOT EXISTS");
        assertThat(Path.of("target/classes/db/migration/V3__generated.sql")).doesNotExist();
    }
}
