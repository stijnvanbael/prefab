package be.appify.prefab.processor.event.handler;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class EventHandlerWriterTest {

    @Test
    void staticEventHandlerGeneratesServiceMethod() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/handler/statichandler/source/Order.java"),
                        sourceOf("event/handler/statichandler/source/OrderCreated.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.handler.statichandler.application.OrderService")
                .contentsAsUtf8String()
                .contains("orderRepository.save(Order.onCreate(event))");
    }

    @Test
    void staticEventHandlerAddsEventListenerAnnotation() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/handler/statichandler/source/Order.java"),
                        sourceOf("event/handler/statichandler/source/OrderCreated.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.handler.statichandler.application.OrderService")
                .contentsAsUtf8String()
                .contains("@EventListener");
    }

    @Test
    void staticEventHandlerWithOptionalReturnUsesIfPresent() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/handler/optionalhandler/source/Order.java"),
                        sourceOf("event/handler/optionalhandler/source/OrderCreated.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.handler.optionalhandler.application.OrderService")
                .contentsAsUtf8String()
                .contains("ifPresent(aggregate -> orderRepository.save(aggregate))");
    }

    @Test
    void staticEventHandlerWithPlatformEventOmitsEventListenerAnnotation() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/handler/platformevent/source/Order.java"),
                        sourceOf("event/handler/platformevent/source/OrderCreated.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.handler.platformevent.application.OrderService")
                .contentsAsUtf8String()
                .doesNotContain("@EventListener");
    }

    @Test
    void multicastEventHandlerQueriesRepository() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/handler/multicast/source/Channel.java"),
                        sourceOf("event/handler/multicast/source/MessageSent.java"),
                        sourceOf("event/handler/multicast/source/ChannelRepositoryMixin.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.handler.multicast.application.ChannelService")
                .contentsAsUtf8String()
                .contains("channelRepository.findByChannel(event.channel())");
    }

    @Test
    void multicastEventHandlerThrowsWhenNoAggregatesFound() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/handler/multicast/source/Channel.java"),
                        sourceOf("event/handler/multicast/source/MessageSent.java"),
                        sourceOf("event/handler/multicast/source/ChannelRepositoryMixin.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.handler.multicast.application.ChannelService")
                .contentsAsUtf8String()
                .contains("throw new IllegalStateException");
    }

    @Test
    void multicastEventHandlerSavesAllAggregates() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/handler/multicast/source/Channel.java"),
                        sourceOf("event/handler/multicast/source/MessageSent.java"),
                        sourceOf("event/handler/multicast/source/ChannelRepositoryMixin.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.handler.multicast.application.ChannelService")
                .contentsAsUtf8String()
                .contains("channelRepository.saveAll(");
    }

    @Test
    void byReferenceEventHandlerLoadsAggregateByReference() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/handler/byreference/source/Channel.java"),
                        sourceOf("event/handler/byreference/source/UserSubscribed.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.handler.byreference.application.ChannelService")
                .contentsAsUtf8String()
                .contains("channelRepository.findById(event.channel()");
    }

    @Test
    void byReferenceEventHandlerSavesAggregateAfterHandling() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/handler/byreference/source/Channel.java"),
                        sourceOf("event/handler/byreference/source/UserSubscribed.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.handler.byreference.application.ChannelService")
                .contentsAsUtf8String()
                .contains("channelRepository.save(aggregate)");
    }

    @Test
    void byReferenceEventHandlerThrowsWhenAggregateNotFound() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/handler/byreference/source/Channel.java"),
                        sourceOf("event/handler/byreference/source/UserSubscribed.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.handler.byreference.application.ChannelService")
                .contentsAsUtf8String()
                .contains(".orElseThrow()");
    }

    @Test
    void mergedEventHandlerGeneratesMethodInAggregateRootService() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/handler/mergedhandler/source/Order.java"),
                        sourceOf("event/handler/mergedhandler/source/OrderCreated.java"),
                        sourceOf("event/handler/mergedhandler/source/OrderSummary.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.handler.mergedhandler.application.OrderService")
                .contentsAsUtf8String()
                .contains("orderSummaryRepository.save(OrderSummary.onOrderCreated(event))");
    }

    @Test
    void mergedEventHandlerInjectsComponentRepositoryIntoAggregateRootService() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/handler/mergedhandler/source/Order.java"),
                        sourceOf("event/handler/mergedhandler/source/OrderCreated.java"),
                        sourceOf("event/handler/mergedhandler/source/OrderSummary.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.handler.mergedhandler.application.OrderService")
                .contentsAsUtf8String()
                .contains("OrderSummaryRepository");
    }

    @Test
    void mergedEventHandlerDoesNotGenerateMethodInComponentService() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/handler/mergedhandler/source/Order.java"),
                        sourceOf("event/handler/mergedhandler/source/OrderCreated.java"),
                        sourceOf("event/handler/mergedhandler/source/OrderSummary.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.handler.mergedhandler.application.OrderSummaryService")
                .contentsAsUtf8String()
                .doesNotContain("onOrderCreated");
    }

    @Test
    void mergedEventHandlerWithNonAggregateRootRaisesCompilerError() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/handler/mergedhandler/nonaggregateroot/NotAnAggregate.java"),
                        sourceOf("event/handler/mergedhandler/nonaggregateroot/OrderCreated.java"),
                        sourceOf("event/handler/mergedhandler/nonaggregateroot/OrderSummary.java")
                );

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("@EventHandler value NotAnAggregate must be annotated with @Aggregate");
    }

    @Test
    void pairedHandlerLoadsAggregateByReference() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/handler/createorupdate/source/ChannelSummary.java"),
                        sourceOf("event/handler/createorupdate/source/MessageSent.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.handler.createorupdate.application.ChannelSummaryService")
                .contentsAsUtf8String()
                .contains("channelSummaryRepository.findById(event.summary()");
    }

    @Test
    void pairedHandlerCallsInstanceMethodWhenAggregateFound() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/handler/createorupdate/source/ChannelSummary.java"),
                        sourceOf("event/handler/createorupdate/source/MessageSent.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.handler.createorupdate.application.ChannelSummaryService")
                .contentsAsUtf8String()
                .contains("var updated = aggregate.onUpdate(event)");
    }

    @Test
    void pairedHandlerCallsStaticMethodWhenAggregateNotFound() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/handler/createorupdate/source/ChannelSummary.java"),
                        sourceOf("event/handler/createorupdate/source/MessageSent.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.handler.createorupdate.application.ChannelSummaryService")
                .contentsAsUtf8String()
                .contains("orElseGet(() -> channelSummaryRepository.save(ChannelSummary.onCreate(event)))");
    }

    @Test
    void pairedHandlerDoesNotGenerateSeparateServiceMethodForStaticHandler() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/handler/createorupdate/source/ChannelSummary.java"),
                        sourceOf("event/handler/createorupdate/source/MessageSent.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.handler.createorupdate.application.ChannelSummaryService")
                .contentsAsUtf8String()
                .doesNotContain("public void onCreate(");
    }

    @Test
    void instanceHandlerOnComponentGeneratesMethodInAggregateRootService() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/handler/instancehandler/source/Order.java"),
                        sourceOf("event/handler/instancehandler/source/OrderCreated.java"),
                        sourceOf("event/handler/instancehandler/source/OrderProjection.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.handler.instancehandler.application.OrderService")
                .contentsAsUtf8String()
                .contains("orderProjection.onOrderCreated(event)");
    }

    @Test
    void instanceHandlerOnComponentInjectsComponentIntoAggregateRootService() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/handler/instancehandler/source/Order.java"),
                        sourceOf("event/handler/instancehandler/source/OrderCreated.java"),
                        sourceOf("event/handler/instancehandler/source/OrderProjection.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.handler.instancehandler.application.OrderService")
                .contentsAsUtf8String()
                .contains("OrderProjection");
    }

    @Test
    void instanceHandlerOnNonComponentRaisesCompilerError() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/handler/instancehandler/noncomponent/Order.java"),
                        sourceOf("event/handler/instancehandler/noncomponent/OrderCreated.java"),
                        sourceOf("event/handler/instancehandler/noncomponent/NotAComponent.java")
                );

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining(
                "Merged @EventHandler instance method onOrderCreated must be on a class annotated with @Component");
    }

    @Test
    void multicastWithStaticCompanionCallsStaticWhenNoAggregatesFound() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/handler/multicastcreateorupdate/source/Channel.java"),
                        sourceOf("event/handler/multicastcreateorupdate/source/MessageSent.java"),
                        sourceOf("event/handler/multicastcreateorupdate/source/ChannelRepositoryMixin.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.handler.multicastcreateorupdate.application.ChannelService")
                .contentsAsUtf8String()
                .contains("channelRepository.save(Channel.onCreate(event))");
    }

    @Test
    void multicastWithStaticCompanionDoesNotThrowWhenNoAggregatesFound() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/handler/multicastcreateorupdate/source/Channel.java"),
                        sourceOf("event/handler/multicastcreateorupdate/source/MessageSent.java"),
                        sourceOf("event/handler/multicastcreateorupdate/source/ChannelRepositoryMixin.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.handler.multicastcreateorupdate.application.ChannelService")
                .contentsAsUtf8String()
                .doesNotContain("throw new IllegalStateException");
    }

    @Test
    void multicastWithStaticCompanionDoesNotGenerateSeparateServiceMethodForStaticHandler() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/handler/multicastcreateorupdate/source/Channel.java"),
                        sourceOf("event/handler/multicastcreateorupdate/source/MessageSent.java"),
                        sourceOf("event/handler/multicastcreateorupdate/source/ChannelRepositoryMixin.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.handler.multicastcreateorupdate.application.ChannelService")
                .contentsAsUtf8String()
                .doesNotContain("public void onCreate(");
    }
}
