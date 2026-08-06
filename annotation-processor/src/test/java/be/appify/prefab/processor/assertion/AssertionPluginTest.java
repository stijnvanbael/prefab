package be.appify.prefab.processor.assertion;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.JavaFileObjects;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import static be.appify.prefab.processor.test.ProcessorTestUtil.classpathOptionsWith;
import static be.appify.prefab.processor.test.ProcessorTestUtil.compileDependencyClasspath;
import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    public static final com.google.testing.compile.Compilation nullableRecordEventCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(sourceOf("assertion/source/NullableRecordEvent.java"));

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
    void responseAssertClassContainsComputedFieldAssertionMethods() {
        assertThat(productCompilation).succeeded();
        assertThat(productCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "",
                        "assertion/infrastructure/http/ProductResponseAssert.java")
                .contentsAsUtf8String()
                .contains("hasTagCount(Integer expected)");
    }

    @Test
    void nestedRecordAssertClassContainsComputedFieldAssertionMethods() {
        assertThat(productCompilation).succeeded();
        assertThat(productCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "",
                        "assertion/ProductMoneyAssert.java")
                .contentsAsUtf8String()
                .contains("hasFormatted(String expected)");
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
        contents.contains("anySatisfy(element -> requirements.accept(SampleRecordSampleElementAssert.assertThat(element)))");
    }

    @Test
    void nullableRecordFieldGeneratesSatisfyingAndNullAssertions() {
        assertThat(nullableRecordEventCompilation).succeeded();
        var contents = assertThat(nullableRecordEventCompilation)
                .generatedFile(StandardLocation.CLASS_OUTPUT, "", "assertion/NullableRecordEventAssert.java")
                .contentsAsUtf8String();
        contents.contains("hasPayloadSatisfying(");
        contents.contains("hasPayloadNull()");
    }

    @Test
    void nullableRecordFieldNullAssertionFailsWhenValueIsNotNull() {
        var generatedSources = nullableRecordEventCompilation.generatedSourceFiles();
        var allSources = Stream.concat(
                Stream.of(sourceOf("assertion/source/NullableRecordEvent.java")),
                Stream.concat(generatedSources.stream(), assertjStubSources())).toArray(JavaFileObject[]::new);
        var outputDirectory = compileSourcesToClasspath(allSources);
        try (var classLoader = new URLClassLoader(new URL[]{outputDirectory.toUri().toURL()},
                Thread.currentThread().getContextClassLoader())) {
            var eventClass = classLoader.loadClass("assertion.NullableRecordEvent");
            var payloadClass = classLoader.loadClass("assertion.NullableRecordEvent$Payload");
            var assertClass = classLoader.loadClass("assertion.NullableRecordEventAssert");
            var payload = payloadClass.getDeclaredConstructor(String.class).newInstance("code");
            var event = eventClass.getDeclaredConstructor(payloadClass).newInstance(payload);
            var assertion = assertClass.getMethod("assertThat", eventClass).invoke(null, event);
            var hasPayloadNullMethod = assertClass.getMethod("hasPayloadNull");

            var invocationException = assertThrows(InvocationTargetException.class,
                    () -> hasPayloadNullMethod.invoke(assertion));
            var assertionError = assertInstanceOf(AssertionError.class, invocationException.getCause());
            assertEquals("Expected payload to be <null> but was <Payload[code=code]>", assertionError.getMessage());
        } catch (IOException | ReflectiveOperationException e) {
            throw new RuntimeException(e);
        } finally {
            deleteRecursively(outputDirectory);
        }
    }

    private static Stream<JavaFileObject> assertjStubSources() {
        return Stream.of(
                JavaFileObjects.forSourceString("org.assertj.core.api.AbstractAssert", """
                        package org.assertj.core.api;

                        public abstract class AbstractAssert<SELF extends AbstractAssert<SELF, ACTUAL>, ACTUAL> {
                            protected final ACTUAL actual;
                            protected final SELF myself;

                            @SuppressWarnings("unchecked")
                            protected AbstractAssert(ACTUAL actual, Class<?> selfType) {
                                this.actual = actual;
                                this.myself = (SELF) this;
                            }

                            public SELF isNotNull() {
                                if (actual == null) {
                                    throw new AssertionError("actual must not be null");
                                }
                                return myself;
                            }

                            protected void failWithMessage(String format, Object... args) {
                                throw new AssertionError(String.format(format, args));
                            }
                        }
                        """),
                JavaFileObjects.forSourceString("org.assertj.core.api.Assertions", """
                        package org.assertj.core.api;

                        public final class Assertions {
                            private Assertions() {
                            }

                            public static <T> ObjectAssert<T> assertThat(T actual) {
                                return new ObjectAssert<>(actual);
                            }
                        }
                        """),
                JavaFileObjects.forSourceString("org.assertj.core.api.ObjectAssert", """
                        package org.assertj.core.api;

                        public class ObjectAssert<T> extends AbstractAssert<ObjectAssert<T>, T> {
                            public ObjectAssert(T actual) {
                                super(actual, ObjectAssert.class);
                            }
                        }
                        """));
    }

    private static Path compileSourcesToClasspath(JavaFileObject... sources) {
        try {
            var outputDirectory = Files.createTempDirectory("prefab-assertion-plugin-test");
            var compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                throw new IllegalStateException("System Java compiler is not available");
            }
            var diagnostics = new DiagnosticCollector<JavaFileObject>();
            try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null,
                    StandardCharsets.UTF_8)) {
                fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(outputDirectory));
                var options = List.of("-classpath", System.getProperty("java.class.path"));
                var task = compiler.getTask(null, fileManager, diagnostics, options, null, List.of(sources));
                if (!Boolean.TRUE.equals(task.call())) {
                    var errors = diagnostics.getDiagnostics().stream().map(Object::toString).toList();
                    throw new IllegalStateException("Compilation failed: " + String.join("\n", errors));
                }
            }
            return outputDirectory;
        } catch (IOException e) {
            throw new RuntimeException(e);
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
