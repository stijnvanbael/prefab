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
}
