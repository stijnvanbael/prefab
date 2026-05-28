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

    @Test
    void responseAssertClassIsGeneratedForAggregate() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("assertion/source/Product.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "",
                        "assertion/infrastructure/http/ProductResponseAssert.java")
                .isNotNull();
    }

    @Test
    void responseAssertClassExtendsAbstractAssert() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("assertion/source/Product.java"));

        assertThat(compilation).succeeded();
        var contents = assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "",
                        "assertion/infrastructure/http/ProductResponseAssert.java")
                .contentsAsUtf8String();
        contents.contains("ProductResponseAssert<SELF extends ProductResponseAssert<SELF>>");
        contents.contains("extends AbstractAssert<SELF, ProductResponse>");
    }

    @Test
    void responseAssertClassContainsStaticAssertThatFactory() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("assertion/source/Product.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "",
                        "assertion/infrastructure/http/ProductResponseAssert.java")
                .contentsAsUtf8String()
                .contains("public static ProductResponseAssert<?> assertThat(ProductResponse actual)");
    }

    @Test
    void responseAssertClassContainsFieldAssertionMethods() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("assertion/source/Product.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "",
                        "assertion/infrastructure/http/ProductResponseAssert.java")
                .contentsAsUtf8String()
                .contains("hasName(String expected)");
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "",
                        "assertion/infrastructure/http/ProductResponseAssert.java")
                .contentsAsUtf8String()
                .contains("hasPrice(Double expected)");
    }

    @Test
    void responseAssertClassContainsListSatisfyingAssertionMethod() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("assertion/source/Product.java"));

        assertThat(compilation).succeeded();
        var contents = assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "",
                        "assertion/infrastructure/http/ProductResponseAssert.java")
                .contentsAsUtf8String();
        contents.contains("hasTagsSatisfying(Consumer<ListAssert<String>> requirements)");
        contents.contains("Objects.requireNonNull(requirements, \"requirements must not be null\")");
        contents.doesNotContain("hasTags(List<String> expected)");
    }

    @Test
    void assertionsFactoryClassIsGeneratedForAggregate() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("assertion/source/Product.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "",
                        "assertion/infrastructure/http/Assertions.java")
                .isNotNull();
    }

    @Test
    void assertionsFactoryContainsAssertThatForResponseType() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("assertion/source/Product.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "",
                        "assertion/infrastructure/http/Assertions.java")
                .contentsAsUtf8String()
                .contains("assertThat(ProductResponse actual)");
    }

    @Test
    void eventAssertClassIsGeneratedForEventType() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("assertion/source/ProductCreated.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "", "assertion/ProductCreatedAssert.java")
                .isNotNull();
    }

    @Test
    void eventAssertClassContainsFieldAssertionMethods() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("assertion/source/ProductCreated.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "", "assertion/ProductCreatedAssert.java")
                .contentsAsUtf8String()
                .contains("hasProductId(String expected)");
    }

    @Test
    void eventAssertionsFactoryClassIsGenerated() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("assertion/source/ProductCreated.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
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
