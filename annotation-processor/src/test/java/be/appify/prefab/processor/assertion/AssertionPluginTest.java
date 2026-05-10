package be.appify.prefab.processor.assertion;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import javax.tools.StandardLocation;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

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
        assertThat(compilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "",
                        "assertion/infrastructure/http/ProductResponseAssert.java")
                .contentsAsUtf8String()
                .contains("extends AbstractAssert<ProductResponseAssert, ProductResponse>");
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
                .contains("public static ProductResponseAssert assertThat(ProductResponse actual)");
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
}
