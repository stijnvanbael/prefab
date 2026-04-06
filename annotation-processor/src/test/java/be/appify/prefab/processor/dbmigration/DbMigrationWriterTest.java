package be.appify.prefab.processor.dbmigration;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import javax.tools.StandardLocation;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.contentsOf;

import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class DbMigrationWriterTest {

    @Test
    void filterFieldGetsIndex() throws IOException {
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
    void indexedUniqueFieldGetsUniqueIndex() throws IOException {
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
    void noIndexForNonAnnotatedField() throws IOException {
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
    void foreignKeyColumnInChildTableGetsIndex() throws IOException {
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
    void notNullConstraintsAreGeneratedForNonNullableFields() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/notnull/source/Product.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "", "db/migration/V1__generated.sql")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("dbmigration/notnull/expected/V1__generated.sql"));
    }
}
