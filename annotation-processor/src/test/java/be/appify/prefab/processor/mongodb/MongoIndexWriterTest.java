package be.appify.prefab.processor.mongodb;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import javax.tools.StandardLocation;
import java.io.IOException;

import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class MongoIndexWriterTest {

    @Test
    void filterFieldGetsIndex() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mongodb/indexed/source/Product.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("mongodb.indexed.infrastructure.mongodb.MongoIndexConfiguration")
                .contentsAsUtf8String()
                .contains("mongoTemplate.indexOps(Product.class).ensureIndex(new Index(\"name\", Direction.ASC))");
    }

    @Test
    void indexedUniqueFieldGetsUniqueIndex() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mongodb/indexed/source/Product.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("mongodb.indexed.infrastructure.mongodb.MongoIndexConfiguration")
                .contentsAsUtf8String()
                .contains("mongoTemplate.indexOps(Product.class).ensureIndex(new Index(\"sku\", Direction.ASC).unique())");
    }

    @Test
    void noIndexForNonAnnotatedField() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mongodb/indexed/source/Product.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("mongodb.indexed.infrastructure.mongodb.MongoIndexConfiguration")
                .contentsAsUtf8String()
                .doesNotContain("\"description\"");
    }

    @Test
    void generatedClassIsConfiguration() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mongodb/indexed/source/Product.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("mongodb.indexed.infrastructure.mongodb.MongoIndexConfiguration")
                .contentsAsUtf8String()
                .contains("@Configuration");
    }

    @Test
    void nestedRecordFieldGetsIndexWithDotPath() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mongodb/nestedindex/source/Product.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("mongodb.nestedindex.infrastructure.mongodb.MongoIndexConfiguration")
                .contentsAsUtf8String()
                .contains("mongoTemplate.indexOps(Product.class).ensureIndex(new Index(\"price.currency\", Direction.ASC))");
    }
}
