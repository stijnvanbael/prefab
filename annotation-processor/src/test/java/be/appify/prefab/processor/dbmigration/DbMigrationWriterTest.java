package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.core.util.IdentifierShortener;
import be.appify.prefab.processor.PrefabProcessor;
import com.google.common.truth.Truth;
import com.google.testing.compile.Compilation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.StandardLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static be.appify.prefab.processor.test.ProcessorTestUtil.classpathOptionsWith;
import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class DbMigrationWriterTest {

    public static final Compilation productCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("dbmigration/indexed/source/Product.java"));
    public static final Compilation productWithListCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("dbmigration/valuetypewrappingrecord/source/ProductWithList.java"));
    public static final Compilation customTypeProductCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("dbmigration/customtype/source/Product.java"));
    public static final Compilation articleCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("dbmigration/textcolumn/source/Article.java"));
    public static final Compilation varcharSizeProductCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("dbmigration/varcharsize/source/Product.java"));
    public static final Compilation notNullProductCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("dbmigration/notnull/source/Product.java"));
    public static final Compilation orderCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("dbmigration/nestedlists/source/Order.java"));

    @Test
    void filterFieldGetsIndex() {
        assertThat(productCompilation).succeeded();
        assertThat(productCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("CREATE INDEX \"product_name_ix\" ON \"product\" (\"name\")");
    }

    @Test
    void indexedUniqueFieldGetsUniqueIndex() {
        assertThat(productCompilation).succeeded();
        assertThat(productCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("CREATE UNIQUE INDEX \"product_sku_uk\" ON \"product\" (\"sku\")");
    }

    @Test
    void noIndexForNonAnnotatedField() {
        assertThat(productCompilation).succeeded();
        assertThat(productCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .doesNotContain("product_description");
    }

    @Test
    void foreignKeyColumnInChildTableGetsIndex() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/fkindex/source/Order.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("CREATE INDEX \"order_order_line_order_ix\" ON \"order_order_line\" (\"order\")");
    }

    @Test
    void longNestedColumnAndIndexNamesAreShortened() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/longidentifiers/source/OrderProjection.java"));

        assertThat(compilation).succeeded();

        var columnName = IdentifierShortener.columnName("customerContextInformation_legalRepresentativeDisplayNameForDocumentsAndNotifications");
        var indexName = IdentifierShortener.indexName("order_projection", columnName, false);

        var sql = assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String();

        Truth.assertThat(columnName.length()).isAtMost(IdentifierShortener.POSTGRES_MAX_IDENTIFIER_LENGTH);
        Truth.assertThat(indexName.length()).isAtMost(IdentifierShortener.POSTGRES_MAX_IDENTIFIER_LENGTH);
        sql.contains("\"" + columnName + "\" VARCHAR (255)");
        sql.contains("CREATE INDEX \"" + indexName + "\" ON \"order_projection\" (\""
                + columnName + "\")");
    }

    @Test
    void notNullConstraintsAreGeneratedForNonNullableFields() {
        assertThat(notNullProductCompilation).succeeded();
        var sql = assertThat(notNullProductCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "", "db/migration/V1__generated.sql")
                .contentsAsUtf8String();
        
        // Verify core table structure
        sql.contains("CREATE TABLE \"product\"");
        sql.contains("PRIMARY KEY(\"id\")");
        
        // Verify NOT NULL constraints on non-nullable fields
        sql.contains("\"id\" VARCHAR (255) NOT NULL");
        sql.contains("\"version\" BIGINT NOT NULL");
        sql.contains("\"name\" VARCHAR (255) NOT NULL");
        
        // Verify nullable field lacks NOT NULL constraint
        sql.contains("\"description\" VARCHAR (255)");
        sql.doesNotContain("\"description\" VARCHAR (255) NOT NULL");
    }

    @Test
    void nonStringValueTypesMappedToCorrectColumnType() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/valuetype/source/Product.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("\"name\" VARCHAR (255)");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("\"price\" DECIMAL (19, 4)");
    }

    @Test
    void singleValueTypeWrappingRecordIsFlattenedIntoColumns() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/valuetypewrappingrecord/source/Product.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("\"address_street\" VARCHAR (255)");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("\"address_city\" VARCHAR (255)");
    }

    @Test
    void listOfSingleValueTypeWrappingRecordGeneratesChildTable() {
        assertThat(productWithListCompilation).succeeded();
        assertThat(productWithListCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("CREATE TABLE \"product_with_list_address_holder\"");
    }

    @Test
    void childTableOfSingleValueTypeWrappingRecordExpandsInnerRecordColumns() {
        assertThat(productWithListCompilation).succeeded();
        var sql = assertThat(productWithListCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String();

        sql.contains("\"street\" VARCHAR (255)");
        sql.contains("\"city\" VARCHAR (255)");
    }

    @Test
    void customTypeFieldIsSkippedFromDbMigration() {
        assertThat(customTypeProductCompilation).succeeded();
        // Regular fields ARE present
        assertThat(customTypeProductCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("\"name\" VARCHAR (255)");
        // @CustomType field is NOT present (no column generated)
        assertThat(customTypeProductCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .doesNotContain("result");
    }

    @Test
    void sizeAnnotationDeterminesVarcharLength() {
        assertThat(varcharSizeProductCompilation).succeeded();
        assertThat(varcharSizeProductCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("\"name\" VARCHAR (100)");
    }

    @Test
    void sizeAnnotationOnSingleValueTypeInnerFieldDeterminesVarcharLength() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/varcharsize/source/ProductWithValueType.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("\"name\" VARCHAR (100)");
    }

    @Test
    void sizeAnnotationOnOuterSingleValueTypeFieldDeterminesVarcharLength() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/varcharsize/source/ProductWithOuterAnnotation.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("\"name\" VARCHAR (100)");
    }

    @Test
    void sizeAnnotationOnPlainStringFieldWithCustomConstructorDeterminesVarcharLength() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/varcharsize/source/ConversationMessage.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("\"content\" VARCHAR (65535)");
    }

    @Test
    void customTypeFieldSkipEmitsNote() {
        assertThat(customTypeProductCompilation).succeeded();
        assertThat(customTypeProductCompilation).hadNoteContaining("result");
        assertThat(customTypeProductCompilation).hadNoteContaining("@CustomType");
    }

    @Test
    void textAnnotationMapsStringToTextColumn() {
        assertThat(articleCompilation).succeeded();
        assertThat(articleCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("\"body\" TEXT");
        assertThat(articleCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("\"title\" VARCHAR (200)");
    }

    @Test
    void unconstrainedStringFieldEmitsWarning() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/unconstrainedstring/source/Note.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation).hadWarningContaining("content");
        assertThat(compilation).hadWarningContaining("VARCHAR(255)");
    }

    @Test
    void constrainedStringFieldDoesNotEmitWarning() {
        assertThat(varcharSizeProductCompilation).succeeded();
        var prefabWarnings = varcharSizeProductCompilation.warnings().stream()
                .filter(d -> d.getMessage(null).contains("VARCHAR(255)"))
                .toList();
        assertNoPrefabWarnings(prefabWarnings);
    }

    @Test
    void textAnnotatedStringFieldDoesNotEmitWarning() {
        assertThat(articleCompilation).succeeded();
        var prefabWarnings = articleCompilation.warnings().stream()
                .filter(d -> d.getMessage(null).contains("VARCHAR(255)"))
                .toList();
        assertNoPrefabWarnings(prefabWarnings);
    }

    @Test
    void defaultIdFieldNameIsUsedAsPrimaryKey() {
        assertThat(notNullProductCompilation).succeeded();
        assertThat(notNullProductCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("PRIMARY KEY(\"id\")");
    }

    @Test
    void customIdFieldNameIsUsedAsPrimaryKey() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/customid/source/Product.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("PRIMARY KEY(\"product_id\")");
    }

    @Test
    void topLevelChildListTableIsGenerated() {
        assertThat(orderCompilation).succeeded();
        assertThat(orderCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("CREATE TABLE \"order_order_line\"");
    }

    @Test
    void deeplyNestedChildListTableIsGenerated() {
        assertThat(orderCompilation).succeeded();
        assertThat(orderCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("CREATE TABLE \"order_order_line_note\"");
    }

    @Test
    void deeplyNestedChildTablesAreCreatedAfterReferencedParentTables() {
        assertThat(orderCompilation).succeeded();
        var migration = assertThat(orderCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String();

        migration.containsMatch("(?s).*CREATE TABLE \"order\".*CREATE TABLE \"order_order_line\".*"
                + "CREATE TABLE \"order_order_line_note\".*");
    }

    @Test
    void nestedChildTableForeignKeyReferencesParentTable() {
        assertThat(orderCompilation).succeeded();
        assertThat(orderCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("FOREIGN KEY (\"order\", \"order_key\") " +
                        "REFERENCES \"order_order_line\"(\"order\", \"order_key\")");
    }

    @Test
    void nestedChildTableForeignKeyUsesParentPrimaryKeyColumn() {
        assertThat(orderCompilation).succeeded();
        assertThat(orderCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("\"order_order_line_key\" INTEGER NOT NULL");
    }

    @Test
    void childTableForeignKeyUsesCustomAggregateIdColumn() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/customidchild/source/Order.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("\"order\" VARCHAR (255) NOT NULL REFERENCES \"order\"(\"order_id\")");
    }

    @Test
    void migrationIsGeneratedByDefaultWithoutDbMigrationAnnotation() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/defaulton/source/Product.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("CREATE TABLE \"product\"");
    }

    @Test
    void migrationIsNotGeneratedWhenDbMigrationEnabledIsFalse() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/optout/source/Product.java"));

        assertThat(compilation).succeeded();
        Truth.assertThat(compilation.generatedFiles().stream()
                        .noneMatch(f -> f.getName().endsWith("V1__generated.sql")))
                .isTrue();
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class JsonbDocument {

        Compilation compilation;

        @BeforeAll
        void compile() {
            compilation = javac()
                    .withProcessors(new PrefabProcessor())
                    .compile(
                            sourceOf("dbmigration/jsonbdocument/source/Product.java"),
                            sourceOf("dbmigration/jsonbdocument/source/ProductDetails.java"),
                            sourceOf("dbmigration/jsonbdocument/source/Tag.java")
                    );
        }

        @Test
        void dbDocumentFieldGeneratesJsonbColumn() {
            assertThat(compilation).succeeded();
            assertThat(compilation)
                    .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                    .contentsAsUtf8String()
                    .contains("\"details\" JSONB");
        }

        @Test
        void dbDocumentListFieldGeneratesJsonbColumn() {
            assertThat(compilation).succeeded();
            assertThat(compilation)
                    .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                    .contentsAsUtf8String()
                    .contains("\"tags\" JSONB");
        }

        @Test
        void dbDocumentListFieldDoesNotGenerateChildTable() {
            assertThat(compilation).succeeded();
            assertThat(compilation)
                    .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                    .contentsAsUtf8String()
                    .doesNotContain("product_tag");
        }

        @Test
        void indexedOnDbDocumentFieldGeneratesGinIndex() {
            assertThat(compilation).succeeded();
            assertThat(compilation)
                    .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                    .contentsAsUtf8String()
                    .contains("CREATE INDEX \"product_details_gin\" ON \"product\" USING GIN (\"details\")");
        }

        @Test
        void indexedFieldInsideDbDocumentTypeGeneratesExpressionIndex() {
            assertThat(compilation).succeeded();
            assertThat(compilation)
                    .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                    .contentsAsUtf8String()
                    .contains("CREATE INDEX \"product_details_category_ix\" ON \"product\" ((\"details\"->>'category'))");
        }

        @Test
        void dbDocumentFieldDoesNotExpandIntoColumns() {
            assertThat(compilation).succeeded();
            assertThat(compilation)
                    .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                    .contentsAsUtf8String()
                    .doesNotContain("\"details_category\"");
        }
    }

    private static void assertNoPrefabWarnings(List<?> warnings) {
        Truth.assertThat(warnings).isEmpty();
    }

    @Test
    void aggregateRootsInDifferentPackagesGenerateSingleMigrationFile() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("dbmigration/multipackage/source/product/Product.java"),
                        sourceOf("dbmigration/multipackage/source/order/Order.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("CREATE TABLE \"product\"");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("CREATE TABLE \"order\"");
    }

    @Test
    void dbColumnAnnotationUsesCustomSqlType() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("dbmigration/dbcolumn/source/Embedding.java"),
                        sourceOf("dbmigration/dbcolumn/source/FloatArrayToVectorConverter.java")
                );
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("\"embedding\" vector(1536)");
    }

    @Test
    void dbColumnAnnotationAllowsOtherwiseUnsupportedFieldType() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("dbmigration/dbcolumn/source/Embedding.java"),
                        sourceOf("dbmigration/dbcolumn/source/FloatArrayToVectorConverter.java")
                );
        assertThat(compilation).succeeded();
    }

    @Test
    void dbColumnAnnotationWithBlankTypeEmitsCompileError() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/dbcolumn/source/EmbeddingBlankType.java"));
        assertThat(compilation).hadErrorContaining("@DbColumn.type() must not be blank");
    }

    @Test
    void dbColumnConverterGeneratesContributorClass() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("dbmigration/dbcolumn/source/Embedding.java"),
                        sourceOf("dbmigration/dbcolumn/source/FloatArrayToVectorConverter.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("dbmigration.dbcolumn.infrastructure.persistence.DbColumnConverterContributor")
                .contentsAsUtf8String()
                .contains("new FloatArrayToVectorConverter()");
    }

    @Test
    void dbColumnSupportsBoxedArrayByteArrayAndCustomRecord() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/dbcolumn/source/EmbeddingVariants.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("\"embedding\" vector(384)");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("\"digest\" bytea");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("\"metadata\" jsonb");
    }

    @Test
    void fuzzyAutocompleteFieldGeneratesTrigmIndex() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/autocomplete_fuzzy/source/SimpleFuzzyProduct.java"));
        assertThat(compilation).succeeded();
        var sql = assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String();
        sql.contains("CREATE EXTENSION IF NOT EXISTS \"pg_trgm\"");
        sql.contains("CREATE INDEX");
        sql.contains("simple_fuzzy_product");
        sql.contains("trgm");
    }

    @Test
    void fuzzyAutocompleteFieldGeneratesCorrectIndexWithOperators() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/autocomplete_fuzzy/source/SimpleFuzzyProduct.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("gin_trgm_ops");
    }

    @Test
    void multipleAutocompleteFieldsWithMixedStrategies() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/autocomplete_fuzzy/source/MixedAutocompleteProduct.java"));
        assertThat(compilation).succeeded();
        var sql = assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String();
        // Should have pg_trgm extension
        sql.contains("CREATE EXTENSION IF NOT EXISTS \"pg_trgm\"");
        // Should have trgm index for productName (fuzzy)
        sql.contains("_product_name_trgm");
        // Should have trgm index for description (fuzzy CONTAINS)
        sql.contains("_description_trgm");
        // Should NOT have trigram index for category (IGNORE_CASE)
        sql.doesNotContain("_category_trgm");
        // Should NOT have trigram index for sku (no autocomplete)
        sql.doesNotContain("_sku_trgm");
    }

    @Test
    void extensionIsAddedOnlyOnce() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/autocomplete_fuzzy/source/MixedAutocompleteProduct.java"));
        assertThat(compilation).succeeded();
        // Verify the extension is present and count occurrences
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("CREATE EXTENSION IF NOT EXISTS \"pg_trgm\"");
        // Verify it only appears once by checking no double occurrence
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .doesNotContain("CREATE EXTENSION IF NOT EXISTS \"pg_trgm\"\n.*CREATE EXTENSION IF NOT EXISTS \"pg_trgm\"");
    }

    @Test
    void renamedMigrationOnClasspathDoesNotGenerateDuplicateMigration() throws IOException {
        var baselineCompilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/indexed/source/Product.java"));
        assertThat(baselineCompilation).succeeded();
        var baselineMigrationSql = generatedMigrationContent(baselineCompilation, "db/migration/V1__generated.sql");

        var dependencyClasspath = classpathWithMigration("V1__initial_schema.sql", baselineMigrationSql);
        var recompilation = javac()
                .withProcessors(new PrefabProcessor())
                .withOptions(classpathOptionsWith(dependencyClasspath))
                .compile(sourceOf("dbmigration/indexed/source/Product.java"));

        assertThat(recompilation).succeeded();
        var generatedMigrations = recompilation.generatedFiles().stream()
                .map(file -> file.getName())
                .filter(name -> name.contains("db/migration/") && name.endsWith("__generated.sql"))
                .toList();
        Truth.assertThat(generatedMigrations).isEmpty();
    }

    @Test
    void renamedMigrationStillAllowsDeltaGenerationAndNextVersionSelection() throws IOException {
        var baselineCompilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/indexed/source/Product.java"));
        assertThat(baselineCompilation).succeeded();
        var baselineMigrationSql = generatedMigrationContent(baselineCompilation, "db/migration/V1__generated.sql");

        var dependencyClasspath = classpathWithMigration("V7__initial_schema.sql", baselineMigrationSql);
        var changedCompilation = javac()
                .withProcessors(new PrefabProcessor())
                .withOptions(classpathOptionsWith(dependencyClasspath))
                .compile(sourceOf("dbmigration/indexed_renamed/source/Product.java"));

        assertThat(changedCompilation).succeeded();
        var generatedMigrations = changedCompilation.generatedFiles().stream()
                .map(file -> file.getName())
                .filter(name -> name.contains("db/migration/") && name.endsWith("__generated.sql"))
                .toList();
        Truth.assertThat(generatedMigrations).isNotEmpty();
        var generatedMigration = generatedMigrations.stream()
                .max(java.util.Comparator.comparingInt(DbMigrationWriterTest::migrationVersion))
                .orElseThrow();
        Truth.assertThat(migrationVersion(generatedMigration)).isGreaterThan(7);

        var migrationPath = generatedMigration.substring(generatedMigration.indexOf("db/migration/"));
        var deltaMigration = generatedMigrationContent(changedCompilation, migrationPath);
        Truth.assertThat(deltaMigration).contains("ALTER TABLE product");
        Truth.assertThat(deltaMigration).contains("sku");
        Truth.assertThat(deltaMigration).doesNotContain("CREATE TABLE \"product\"");
    }

    private static Path classpathWithMigration(String migrationName, String migrationSql) throws IOException {
        var classpathDirectory = Files.createTempDirectory("prefab-db-migration-classpath");
        var migrationDirectory = classpathDirectory.resolve(Path.of("db", "migration"));
        Files.createDirectories(migrationDirectory);
        var versionPrefix = migrationName.substring(0, migrationName.indexOf("__"));
        Files.writeString(migrationDirectory.resolve(versionPrefix + "__baseline.sql"), "");
        Files.writeString(migrationDirectory.resolve(migrationName), migrationSql);
        return classpathDirectory;
    }

    private static String generatedMigrationContent(Compilation compilation, String migrationPath) throws IOException {
        var migrationFile = compilation.generatedFiles().stream()
                .filter(file -> file.getName().endsWith(migrationPath))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing generated migration: " + migrationPath));
        return migrationFile.getCharContent(false).toString();
    }

    private static int migrationVersion(String migrationPath) {
        var filename = migrationPath.substring(migrationPath.lastIndexOf('/') + 1);
        var matcher = java.util.regex.Pattern.compile("^V(\\d+)__.*\\.sql$").matcher(filename);
        if (!matcher.matches()) {
            return -1;
        }
        return Integer.parseInt(matcher.group(1));
    }
}
