package be.appify.prefab.processor.mongodb;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import javax.tools.StandardLocation;
import java.io.IOException;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class MongoMigrationPluginTest {

    @Test
    void dbMigrationAnnotationGeneratesMongoMigrationWhenMongoOnClasspath() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mongodb/migration/source/Product.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "mongo/migration/V1__generated.js")
                .contentsAsUtf8String()
                .contains("$rename");
    }

    @Test
    void dbMigrationAnnotationGeneratesFieldRenameInMongoMigration() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mongodb/migration/source/Product.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "mongo/migration/V1__generated.js")
                .contentsAsUtf8String()
                .contains("\"firstName\": \"givenName\"");
    }
}
