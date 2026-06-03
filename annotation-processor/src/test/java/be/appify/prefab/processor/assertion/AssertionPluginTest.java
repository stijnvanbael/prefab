package be.appify.prefab.processor.assertion;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import javax.tools.StandardLocation;

import static be.appify.prefab.processor.test.ProcessorTestUtil.classpathOptionsWith;
import static be.appify.prefab.processor.test.ProcessorTestUtil.compileDependencyClasspath;
import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AssertionPluginTest {

    public static final com.google.testing.compile.Compilation productCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("assertion/source/Product.java"));
    public static final com.google.testing.compile.Compilation productCreatedCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("assertion/source/ProductCreated.java"));
    public static final com.google.testing.compile.Compilation sampleRecordCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("assertion/source/SampleRecord.java"));

    @Test
    void responseAssertClassIsGeneratedForAggregate() {
        assertThat(productCompilation).succeeded();
        assertThat(productCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "",
                        "assertion/infrastructure/http/ProductResponseAssert.java")
                .isNotNull();
    }

    @Test
    void responseAssertClassExtendsAbstractAssert() {
        assertThat(productCompilation).succeeded();
        var contents = assertThat(productCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "",
                        "assertion/infrastructure/http/ProductResponseAssert.java")
                .contentsAsUtf8String();
        contents.contains("ProductResponseAssert<SELF extends ProductResponseAssert<SELF>>");
        contents.contains("extends AbstractAssert<SELF, ProductResponse>");
    }

    @Test
    void responseAssertClassContainsStaticAssertThatFactory() {
        assertThat(productCompilation).succeeded();
        assertThat(productCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "",
                        "assertion/infrastructure/http/ProductResponseAssert.java")
                .contentsAsUtf8String()
                .contains("public static ProductResponseAssert<?> assertThat(ProductResponse actual)");
    }

    @Test
    void responseAssertClassContainsFieldAssertionMethods() {
        assertThat(productCompilation).succeeded();
        assertThat(productCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "",
                        "assertion/infrastructure/http/ProductResponseAssert.java")
                .contentsAsUtf8String()
                .contains("hasName(String expected)");
        assertThat(productCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "",
                        "assertion/infrastructure/http/ProductResponseAssert.java")
                .contentsAsUtf8String()
                .contains("hasPrice(Double expected)");
    }

    @Test
    void responseAssertClassContainsListSatisfyingAssertionMethod() {
        assertThat(productCompilation).succeeded();
        var contents = assertThat(productCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "",
                        "assertion/infrastructure/http/ProductResponseAssert.java")
                .contentsAsUtf8String();
        contents.contains("hasTagsSatisfying(Consumer<ListAssert<String>> requirements)");
        contents.contains("Objects.requireNonNull(requirements, \"requirements must not be null\")");
        contents.doesNotContain("hasTags(List<String> expected)");
    }

    @Test
    void assertionsFactoryClassIsGeneratedForAggregate() {
        assertThat(productCompilation).succeeded();
        assertThat(productCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "",
                        "assertion/infrastructure/http/Assertions.java")
                .isNotNull();
    }

    @Test
    void assertionsFactoryContainsAssertThatForResponseType() {
        assertThat(productCompilation).succeeded();
        assertThat(productCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "",
                        "assertion/infrastructure/http/Assertions.java")
                .contentsAsUtf8String()
                .contains("assertThat(ProductResponse actual)");
    }

    @Test
    void assertClassIsGeneratedForSingleValueType() {
        assertThat(productCompilation).succeeded();
        assertThat(productCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "", "assertion/ProductMoneyAssert.java")
                .isNotNull();
        assertThat(productCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "", "be/appify/prefab/core/service/ReferenceAssert.java")
                .isNotNull();
    }

    @Test
    void eventAssertClassIsGeneratedForEventType() {
        assertThat(productCreatedCompilation).succeeded();
        assertThat(productCreatedCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "", "assertion/ProductCreatedAssert.java")
                .isNotNull();
    }

    @Test
    void eventAssertClassContainsFieldAssertionMethods() {
        assertThat(productCreatedCompilation).succeeded();
        assertThat(productCreatedCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "", "assertion/ProductCreatedAssert.java")
                .contentsAsUtf8String()
                .contains("hasProductId(String expected)");
    }

    @Test
    void eventAssertionsFactoryClassIsGenerated() {
        assertThat(productCreatedCompilation).succeeded();
        assertThat(productCreatedCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "", "assertion/Assertions.java")
                .isNotNull();
    }

    @Test
    void dependencyEventDoesNotGenerateAssertionClassesInConsumerModule() {
        var dependencyClasspath = compileDependencyClasspath(
                sourceOf("event/serialization/dependency/source/DependencyEvent.java"));
        try {
            var compilation = javac()
                    .withOptions(classpathOptionsWith(dependencyClasspath))
                    .withProcessors(new PrefabProcessor())
                    .compile(sourceOf("event/serialization/dependencyconsumer/source/DependencyConsumer.java"));

            assertThat(compilation).succeeded();
            assertFalse(compilation.generatedSourceFiles().stream().anyMatch(file -> file.toUri().getPath().endsWith(
                    "/event/serialization/dependency/DependencyEventAssert.java")));
            assertFalse(compilation.generatedSourceFiles().stream().anyMatch(file -> file.toUri().getPath().endsWith(
                    "/event/serialization/dependency/Assertions.java")));
        } finally {
            deleteRecursively(dependencyClasspath);
        }
    }

    @Test
    void listFieldNameEndingWithListGeneratesListSatisfyingMethod() {
        assertThat(sampleRecordCompilation).succeeded();
        assertThat(sampleRecordCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "",
                        "assertion/infrastructure/http/SampleRecordResponseAssert.java")
                .contentsAsUtf8String()
                .contains("hasSampleElementListSatisfying(");
    }

    @Test
    void listFieldNameEndingWithListGeneratesElementSatisfyingMethod() {
        assertThat(sampleRecordCompilation).succeeded();
        var contents = assertThat(sampleRecordCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "",
                        "assertion/infrastructure/http/SampleRecordResponseAssert.java")
                .contentsAsUtf8String();
        contents.contains("hasSampleRecordSampleElementSatisfying(");
        contents.contains("SampleRecordSampleElementAssert.assertThat(element)");
    }

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
