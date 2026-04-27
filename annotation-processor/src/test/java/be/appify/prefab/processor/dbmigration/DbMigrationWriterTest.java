package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.core.util.IdentifierShortener;
import be.appify.prefab.processor.PrefabProcessor;
import com.google.common.truth.Truth;
import com.google.testing.compile.Compilation;
import java.util.List;
import javax.tools.StandardLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.contentsOf;
import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class DbMigrationWriterTest {

    @Test
    void filterFieldGetsIndex() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/indexed/source/Product.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("CREATE INDEX \"product_name_ix\" ON \"product\" (\"name\")");
    }

    @Test
    void indexedUniqueFieldGetsUniqueIndex() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/indexed/source/Product.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("CREATE UNIQUE INDEX \"product_sku_uk\" ON \"product\" (\"sku\")");
    }

    @Test
    void noIndexForNonAnnotatedField() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/indexed/source/Product.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
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
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/notnull/source/Product.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "", "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("dbmigration/notnull/expected/V1__generated.sql"));
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
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/valuetypewrappingrecord/source/ProductWithList.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("CREATE TABLE \"product_with_list_address_holder\"");
    }

    @Test
    void childTableOfSingleValueTypeWrappingRecordExpandsInnerRecordColumns() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/valuetypewrappingrecord/source/ProductWithList.java"));

        assertThat(compilation).succeeded();
        var sql = assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String();

        sql.contains("\"street\" VARCHAR (255)");
        sql.contains("\"city\" VARCHAR (255)");
    }

    @Test
    void customTypeFieldIsSkippedFromDbMigration() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/customtype/source/Product.java"));

        assertThat(compilation).succeeded();
        // Regular fields ARE present
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("\"name\" VARCHAR (255)");
        // @CustomType field is NOT present (no column generated)
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .doesNotContain("result");
    }

    @Test
    void sizeAnnotationDeterminesVarcharLength() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/varcharsize/source/Product.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
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
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/customtype/source/Product.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation).hadNoteContaining("result");
        assertThat(compilation).hadNoteContaining("@CustomType");
    }

    @Test
    void textAnnotationMapsStringToTextColumn() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/textcolumn/source/Article.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("\"body\" TEXT");
        assertThat(compilation)
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
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/varcharsize/source/Product.java"));

        assertThat(compilation).succeeded();
        var prefabWarnings = compilation.warnings().stream()
                .filter(d -> d.getMessage(null).contains("VARCHAR(255)"))
                .toList();
        assertNoPrefabWarnings(prefabWarnings);
    }

    @Test
    void textAnnotatedStringFieldDoesNotEmitWarning() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/textcolumn/source/Article.java"));

        assertThat(compilation).succeeded();
        var prefabWarnings = compilation.warnings().stream()
                .filter(d -> d.getMessage(null).contains("VARCHAR(255)"))
                .toList();
        assertNoPrefabWarnings(prefabWarnings);
    }

    @Test
    void defaultIdFieldNameIsUsedAsPrimaryKey() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/notnull/source/Product.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
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
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/nestedlists/source/Order.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("CREATE TABLE \"order_order_line\"");
    }

    @Test
    void deeplyNestedChildListTableIsGenerated() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/nestedlists/source/Order.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("CREATE TABLE \"order_order_line_note\"");
    }

    @Test
    void deeplyNestedChildTablesAreCreatedAfterReferencedParentTables() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/nestedlists/source/Order.java"));

        assertThat(compilation).succeeded();
        var migration = assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String();

        migration.containsMatch("(?s).*CREATE TABLE \\\"order\\\".*CREATE TABLE \\\"order_order_line\\\".*"
                + "CREATE TABLE \\\"order_order_line_note\\\".*");
    }

    @Test
    void nestedChildTableForeignKeyReferencesParentTable() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/nestedlists/source/Order.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .contains("FOREIGN KEY (\"order\", \"order_key\") " +
                        "REFERENCES \"order_order_line\"(\"order\", \"order_key\")");
    }

    @Test
    void nestedChildTableForeignKeyUsesParentPrimaryKeyColumn() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/nestedlists/source/Order.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
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
}
