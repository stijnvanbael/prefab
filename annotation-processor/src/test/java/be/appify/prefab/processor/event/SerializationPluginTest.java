package be.appify.prefab.processor.event;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import static be.appify.prefab.processor.test.ProcessorTestUtil.classpathOptionsWith;
import static be.appify.prefab.processor.test.ProcessorTestUtil.compileDependencyClasspath;
import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class SerializationPluginTest {

    @Test
    void singlePackageEventGeneratesOneConfiguration() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/serialization/multipackage/source/order/OrderCreated.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile(
                        "event.serialization.multipackage.order.infrastructure.event.EventSerializationMultipackageOrderSerializationRegistryConfiguration")
                .contentsAsUtf8String()
                .contains("eventSerializationMultipackageOrderSerializationRegistryCustomizer");
    }

    @Test
    void multiPackageEventsGenerateOneConfigurationPerPackage() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/serialization/multipackage/source/order/OrderCreated.java"),
                        sourceOf("event/serialization/multipackage/source/payment/PaymentProcessed.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile(
                        "event.serialization.multipackage.order.infrastructure.event.EventSerializationMultipackageOrderSerializationRegistryConfiguration")
                .contentsAsUtf8String()
                .contains("order-created");
        assertThat(compilation)
                .generatedSourceFile(
                        "event.serialization.multipackage.payment.infrastructure.event.EventSerializationMultipackagePaymentSerializationRegistryConfiguration")
                .contentsAsUtf8String()
                .contains("payment-processed");
    }

    @Test
    void multiPackageEventsUseBeanNameDerivedFromOwnPackage() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/serialization/multipackage/source/order/OrderCreated.java"),
                        sourceOf("event/serialization/multipackage/source/payment/PaymentProcessed.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile(
                        "event.serialization.multipackage.order.infrastructure.event.EventSerializationMultipackageOrderSerializationRegistryConfiguration")
                .contentsAsUtf8String()
                .contains("eventSerializationMultipackageOrderSerializationRegistryCustomizer");
        assertThat(compilation)
                .generatedSourceFile(
                        "event.serialization.multipackage.payment.infrastructure.event.EventSerializationMultipackagePaymentSerializationRegistryConfiguration")
                .contentsAsUtf8String()
                .contains("eventSerializationMultipackagePaymentSerializationRegistryCustomizer");
    }

    @Test
    void nestedEventInterfaceGeneratesSerializationRegistryConfiguration() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("event/serialization/nested/source/sale/Sale.java"));

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile(
                        "event.serialization.nested.sale.infrastructure.event.EventSerializationNestedSaleSerializationRegistryConfiguration")
                .contentsAsUtf8String()
                .contains("sale");
    }

    @Test
    void dependencyEventDoesNotGenerateSerializationRegistryConfiguration() {
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
                            + "EventSerializationDependencySerializationRegistryConfiguration.java")));
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
