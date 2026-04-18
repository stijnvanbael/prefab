package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.common.truth.Truth;
import java.util.List;
import javax.tools.StandardLocation;
import org.junit.jupiter.api.Test;

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
                .contains("CREATE INDEX \"product_name_idx\" ON \"product\" (\"name\")");
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
                .contains("CREATE UNIQUE INDEX \"product_sku_uidx\" ON \"product\" (\"sku\")");
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
                .contains("CREATE INDEX \"order_order_line_order_idx\" ON \"order_order_line\" (\"order\")");
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

    private static void assertNoPrefabWarnings(List<?> warnings) {
        Truth.assertThat(warnings).isEmpty();
    }
}
