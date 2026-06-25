package be.appify.prefab.processor.event;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import com.google.testing.compile.Compilation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import static be.appify.prefab.processor.test.ProcessorTestUtil.classpathOptionsWith;
import static be.appify.prefab.processor.test.ProcessorTestUtil.compileDependencyClasspath;
import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class SerializationPluginTest {

    public static final Compilation multipackageCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(
                    sourceOf("event/serialization/multipackage/source/order/OrderCreated.java"),
                    sourceOf("event/serialization/multipackage/source/payment/PaymentProcessed.java")
            );

    @Test
    void singlePackageEventGeneratesOneRegistrar() {
        assertThat(multipackageCompilation).succeeded();
        assertThat(multipackageCompilation)
                .generatedSourceFile(
                        "event.serialization.multipackage.order.infrastructure.event.OrderCreatedEventTypeRegistrar")
                .contentsAsUtf8String()
                .contains("implements EventRegistryCustomizer");
    }

    @Test
    void multiPackageEventsGenerateOneRegistrarPerType() {
        assertThat(multipackageCompilation).succeeded();
        assertThat(multipackageCompilation)
                .generatedSourceFile(
                        "event.serialization.multipackage.order.infrastructure.event.OrderCreatedEventTypeRegistrar")
                .contentsAsUtf8String()
                .contains("order-created");
        assertThat(multipackageCompilation)
                .generatedSourceFile(
                        "event.serialization.multipackage.payment.infrastructure.event.PaymentProcessedEventTypeRegistrar")
                .contentsAsUtf8String()
                .contains("payment-processed");
    }

    @Test
    void multiPackageEventsUseClassNameDerivedFromType() {
        assertThat(multipackageCompilation).succeeded();
        assertThat(multipackageCompilation)
                .generatedSourceFile(
                        "event.serialization.multipackage.order.infrastructure.event.OrderCreatedEventTypeRegistrar")
                .contentsAsUtf8String()
                .contains("OrderCreatedEventTypeRegistrar");
        assertThat(multipackageCompilation)
                .generatedSourceFile(
                        "event.serialization.multipackage.payment.infrastructure.event.PaymentProcessedEventTypeRegistrar")
                .contentsAsUtf8String()
                .contains("PaymentProcessedEventTypeRegistrar");
    }

    @Test
    void nestedEventInterfaceGeneratesRegistrar() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/serialization/nested/source/sale/Sale.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile(
                        "event.serialization.nested.sale.infrastructure.event.SaleEventsEventTypeRegistrar")
                .contentsAsUtf8String()
                .contains("sale");
    }

    @Test
    void dependencyEventDoesNotGenerateRegistrar() {
        var dependencyClasspath = compileDependencyClasspath(
                sourceOf("event/serialization/dependency/source/DependencyEvent.java"));
        try {
            var compilation = javac()
                    .withOptions(classpathOptionsWith(dependencyClasspath))
                    .withProcessors(new PrefabProcessor())
                    .compile(sourceOf("event/serialization/dependencyconsumer/source/DependencyConsumer.java"));

            assertThat(compilation).succeeded();
            assertFalse(compilation.generatedSourceFiles().stream().anyMatch(file -> file.toUri().getPath().endsWith(
                    "/event/serialization/dependency/infrastructure/event/"
                            + "DependencyEventEventTypeRegistrar.java")));
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
