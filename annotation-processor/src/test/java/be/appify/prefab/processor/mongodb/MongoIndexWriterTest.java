package be.appify.prefab.processor.mongodb;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import javax.tools.StandardLocation;
import java.io.IOException;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class MongoIndexWriterTest {

    @Test
    void filterFieldGetsIndex() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mongodb/indexed/source/Product.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("mongodb.indexed.infrastructure.mongodb.MongoIndexConfiguration")
                .contentsAsUtf8String()
                .contains("mongoTemplate.indexOps(Product.class).createIndex(new Index(\"name\", Direction.ASC))");
    }

    @Test
    void indexedUniqueFieldGetsUniqueIndex() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mongodb/indexed/source/Product.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("mongodb.indexed.infrastructure.mongodb.MongoIndexConfiguration")
                .contentsAsUtf8String()
                .contains("mongoTemplate.indexOps(Product.class).createIndex(new Index(\"sku\", Direction.ASC).unique())");
    }

    @Test
    void noIndexForNonAnnotatedField() {
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
    void fullTextFilterLanguageIsUsedByTheTextIndex() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/fulltextlanguage/source/Pamphlet.java"));
        assertThat(compilation).succeeded();
        var config = assertThat(compilation)
                .generatedSourceFile("dbmigration.fulltextlanguage.infrastructure.mongodb.MongoIndexConfiguration")
                .contentsAsUtf8String();
        config.contains("MongoTextIndexes.createOrRebuild(mongoTemplate.indexOps(Pamphlet.class)");
        config.contains(".withDefaultLanguage(FullTextLanguages.mongoNameOf(\"english\"))");
    }

    @Test
    void fullTextWithoutLanguageUsesTheApplicationDefaultProperty() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("dbmigration/fulltextweighted/source/Booklet.java"));
        assertThat(compilation).succeeded();
        var config = assertThat(compilation)
                .generatedSourceFile("dbmigration.fulltextweighted.infrastructure.mongodb.MongoIndexConfiguration")
                .contentsAsUtf8String();
        config.contains("@Value(\"${prefab.full-text.language:simple}\") String fullTextLanguage");
        config.contains(".withDefaultLanguage(FullTextLanguages.mongoNameOf(fullTextLanguage))");
    }

    @Test
    void generatedClassIsConfiguration() {
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
    void nestedRecordFieldGetsIndexWithDotPath() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("mongodb/nestedindex/source/Product.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("mongodb.nestedindex.infrastructure.mongodb.MongoIndexConfiguration")
                .contentsAsUtf8String()
                .contains("mongoTemplate.indexOps(Product.class).createIndex(new Index(\"price.currency\", Direction.ASC))");
    }
}
