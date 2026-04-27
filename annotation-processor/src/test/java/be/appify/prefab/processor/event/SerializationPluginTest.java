package be.appify.prefab.processor.event;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.sourceOf;
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
                        "event.serialization.multipackage.order.infrastructure.event.SerializationRegistryConfiguration")
                .contentsAsUtf8String()
                .contains("eventSerializationMultipackageOrderSerializationRegistryCustomizer");
    }

    @Test
    void multiPackageEventsGenerateSingleConfigurationInCommonRootPackage() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/serialization/multipackage/source/order/OrderCreated.java"),
                        sourceOf("event/serialization/multipackage/source/payment/PaymentProcessed.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile(
                        "event.serialization.multipackage.infrastructure.event.SerializationRegistryConfiguration")
                .contentsAsUtf8String()
                .contains("order-created");
        assertThat(compilation)
                .generatedSourceFile(
                        "event.serialization.multipackage.infrastructure.event.SerializationRegistryConfiguration")
                .contentsAsUtf8String()
                .contains("payment-processed");
    }

    @Test
    void multiPackageEventsUseBeanNameDerivedFromCommonRootPackage() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/serialization/multipackage/source/order/OrderCreated.java"),
                        sourceOf("event/serialization/multipackage/source/payment/PaymentProcessed.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile(
                        "event.serialization.multipackage.infrastructure.event.SerializationRegistryConfiguration")
                .contentsAsUtf8String()
                .contains("eventSerializationMultipackageSerializationRegistryCustomizer");
    }
}



