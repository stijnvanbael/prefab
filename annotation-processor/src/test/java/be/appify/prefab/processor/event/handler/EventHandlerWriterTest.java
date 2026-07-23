package be.appify.prefab.processor.event.handler;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.test.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class EventHandlerWriterTest {

    public static final com.google.testing.compile.Compilation orderCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(
                    sourceOf("event/handler/statichandler/source/Order.java"),
                    sourceOf("event/handler/statichandler/source/OrderCreated.java")
            );
    public static final com.google.testing.compile.Compilation multicastChannelCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(
                    sourceOf("event/handler/multicast/source/Channel.java"),
                    sourceOf("event/handler/multicast/source/MessageSent.java"),
                    sourceOf("event/handler/multicast/source/ChannelRepositoryMixin.java")
            );
    public static final com.google.testing.compile.Compilation byReferenceChannelCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(
                    sourceOf("event/handler/byreference/source/Channel.java"),
                    sourceOf("event/handler/byreference/source/UserSubscribed.java")
            );
    public static final com.google.testing.compile.Compilation byReferenceMethodPropertyCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(
                    sourceOf("event/handler/byreferencemethodproperty/source/Channel.java"),
                    sourceOf("event/handler/byreferencemethodproperty/source/UserSubscribed.java")
            );
    public static final com.google.testing.compile.Compilation mergedHandlerOrderCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(
                    sourceOf("event/handler/mergedhandler/source/Order.java"),
                    sourceOf("event/handler/mergedhandler/source/OrderCreated.java"),
                    sourceOf("event/handler/mergedhandler/source/OrderSummary.java")
            );
    public static final com.google.testing.compile.Compilation createOrUpdateChannelCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(
                    sourceOf("event/handler/createorupdate/source/ChannelSummary.java"),
                    sourceOf("event/handler/createorupdate/source/MessageEvent.java"),
                    sourceOf("event/handler/createorupdate/source/MessageSent.java")
            );
    public static final com.google.testing.compile.Compilation instanceHandlerOrderCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(
                    sourceOf("event/handler/instancehandler/source/Order.java"),
                    sourceOf("event/handler/instancehandler/source/OrderCreated.java"),
                    sourceOf("event/handler/instancehandler/source/OrderProjection.java")
            );
    public static final com.google.testing.compile.Compilation multicastCreateOrUpdateChannelCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(
                    sourceOf("event/handler/multicastcreateorupdate/source/Channel.java"),
                    sourceOf("event/handler/multicastcreateorupdate/source/MessageEvent.java"),
                    sourceOf("event/handler/multicastcreateorupdate/source/MessageSent.java"),
                    sourceOf("event/handler/multicastcreateorupdate/source/ChannelRepositoryMixin.java")
            );
    public static final com.google.testing.compile.Compilation staticHandlerAuditOrderCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(
                    sourceOf("event/handler/statichandleraudit/source/Order.java"),
                    sourceOf("event/handler/statichandleraudit/source/OrderCreated.java")
            );
    public static final com.google.testing.compile.Compilation staticHandlerSupertypeCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(
                    sourceOf("event/handler/statichandlersupertype/source/Order.java"),
                    sourceOf("event/handler/statichandlersupertype/source/OrderEvent.java")
            );
    public static final com.google.testing.compile.Compilation mergedHandlerSupertypeCompilation = javac()
            .withProcessors(new PrefabProcessor())
            .compile(
                    sourceOf("event/handler/mergedhandlersupertype/source/Order.java"),
                    sourceOf("event/handler/mergedhandlersupertype/source/OrderEvent.java"),
                    sourceOf("event/handler/mergedhandlersupertype/source/OrderSummary.java")
            );

    @Test
    void staticEventHandlerGeneratesServiceMethod() {
        assertThat(orderCompilation).succeeded();
        assertThat(orderCompilation)
                .generatedSourceFile("event.handler.statichandler.application.OrderService")
                .contentsAsUtf8String()
                .contains("orderRepository.save(Order.onCreate(event))");
    }

    @Test
    void staticEventHandlerAddsEventListenerAnnotation() {
        assertThat(orderCompilation).succeeded();
        assertThat(orderCompilation)
                .generatedSourceFile("event.handler.statichandler.application.OrderService")
                .contentsAsUtf8String()
                .contains("@EventListener");
    }

    @Test
    void staticEventHandlerWithSupertypeParameterGeneratesServiceMethod() {
        assertThat(staticHandlerSupertypeCompilation).succeeded();
        assertThat(staticHandlerSupertypeCompilation)
                .generatedSourceFile("event.handler.statichandlersupertype.application.OrderService")
                .contentsAsUtf8String()
                .contains("public void onCreate(OrderEvent event)");
        assertThat(staticHandlerSupertypeCompilation)
                .generatedSourceFile("event.handler.statichandlersupertype.application.OrderService")
                .contentsAsUtf8String()
                .contains("orderRepository.save(Order.onCreate(event))");
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
        assertThat(multicastChannelCompilation).succeeded();
        assertThat(multicastChannelCompilation)
                .generatedSourceFile("event.handler.multicast.application.ChannelService")
                .contentsAsUtf8String()
                .contains("channelRepository.findByChannel(event.channel())");
    }

    @Test
    void multicastEventHandlerThrowsWhenNoAggregatesFound() {
        assertThat(multicastChannelCompilation).succeeded();
        assertThat(multicastChannelCompilation)
                .generatedSourceFile("event.handler.multicast.application.ChannelService")
                .contentsAsUtf8String()
                .contains("throw new IllegalStateException");
    }

    @Test
    void multicastEventHandlerSavesAllAggregates() {
        assertThat(multicastChannelCompilation).succeeded();
        assertThat(multicastChannelCompilation)
                .generatedSourceFile("event.handler.multicast.application.ChannelService")
                .contentsAsUtf8String()
                .contains("channelRepository.saveAll(");
    }

    @Test
    void byReferenceEventHandlerLoadsAggregateByReference() {
        assertThat(byReferenceChannelCompilation).succeeded();
        assertThat(byReferenceChannelCompilation)
                .generatedSourceFile("event.handler.byreference.application.ChannelService")
                .contentsAsUtf8String()
                .contains("channelRepository.findById(event.channel()");
    }

    @Test
    void byReferenceEventHandlerSavesAggregateAfterHandling() {
        assertThat(byReferenceChannelCompilation).succeeded();
        assertThat(byReferenceChannelCompilation)
                .generatedSourceFile("event.handler.byreference.application.ChannelService")
                .contentsAsUtf8String()
                .contains("channelRepository.save(aggregate)");
    }

    @Test
    void byReferenceEventHandlerThrowsWhenAggregateNotFound() {
        assertThat(byReferenceChannelCompilation).succeeded();
        assertThat(byReferenceChannelCompilation)
                .generatedSourceFile("event.handler.byreference.application.ChannelService")
                .contentsAsUtf8String()
                .contains(".orElseThrow()");
    }

    @Test
    void byReferenceEventHandlerResolvesMethodProperty() {
        assertThat(byReferenceMethodPropertyCompilation).succeeded();
        assertThat(byReferenceMethodPropertyCompilation)
                .generatedSourceFile("event.handler.byreferencemethodproperty.application.ChannelService")
                .contentsAsUtf8String()
                .contains("channelRepository.findById(event.channelId()");
    }

    @Test
    void byReferenceEventHandlerWithMethodPropertySavesAggregate() {
        assertThat(byReferenceMethodPropertyCompilation).succeeded();
        assertThat(byReferenceMethodPropertyCompilation)
                .generatedSourceFile("event.handler.byreferencemethodproperty.application.ChannelService")
                .contentsAsUtf8String()
                .contains("channelRepository.save(aggregate)");
    }

    @Test
    void mergedEventHandlerGeneratesMethodInAggregateRootService() {
        assertThat(mergedHandlerOrderCompilation).succeeded();
        assertThat(mergedHandlerOrderCompilation)
                .generatedSourceFile("event.handler.mergedhandler.application.OrderService")
                .contentsAsUtf8String()
                .contains("orderSummaryRepository.save(OrderSummary.onOrderCreated(event))");
    }

    @Test
    void mergedEventHandlerInjectsComponentRepositoryIntoAggregateRootService() {
        assertThat(mergedHandlerOrderCompilation).succeeded();
        assertThat(mergedHandlerOrderCompilation)
                .generatedSourceFile("event.handler.mergedhandler.application.OrderService")
                .contentsAsUtf8String()
                .contains("OrderSummaryRepository");
    }

    @Test
    void mergedEventHandlerDoesNotGenerateMethodInComponentService() {
        assertThat(mergedHandlerOrderCompilation).succeeded();
        assertThat(mergedHandlerOrderCompilation)
                .generatedSourceFile("event.handler.mergedhandler.application.OrderSummaryService")
                .contentsAsUtf8String()
                .doesNotContain("onOrderCreated");
    }

    @Test
    void mergedEventHandlerWithSupertypeParameterGeneratesMethodInAggregateRootService() {
        assertThat(mergedHandlerSupertypeCompilation).succeeded();
        assertThat(mergedHandlerSupertypeCompilation)
                .generatedSourceFile("event.handler.mergedhandlersupertype.application.OrderService")
                .contentsAsUtf8String()
                .contains("public void onOrderCreated(OrderEvent event)");
        assertThat(mergedHandlerSupertypeCompilation)
                .generatedSourceFile("event.handler.mergedhandlersupertype.application.OrderService")
                .contentsAsUtf8String()
                .contains("orderSummaryRepository.save(OrderSummary.onOrderCreated(event))");
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
        assertThat(createOrUpdateChannelCompilation).succeeded();
        assertThat(createOrUpdateChannelCompilation)
                .generatedSourceFile("event.handler.createorupdate.application.ChannelSummaryService")
                .contentsAsUtf8String()
                .contains("channelSummaryRepository.findById(event.summary()");
    }

    @Test
    void pairedHandlerCallsInstanceMethodWhenAggregateFound() {
        assertThat(createOrUpdateChannelCompilation).succeeded();
        assertThat(createOrUpdateChannelCompilation)
                .generatedSourceFile("event.handler.createorupdate.application.ChannelSummaryService")
                .contentsAsUtf8String()
                .contains("public void onUpdate(MessageEvent event)");
        assertThat(createOrUpdateChannelCompilation)
                .generatedSourceFile("event.handler.createorupdate.application.ChannelSummaryService")
                .contentsAsUtf8String()
                .contains("var updated = aggregate.onUpdate(event)");
    }

    @Test
    void pairedHandlerCallsStaticMethodWhenAggregateNotFound() {
        assertThat(createOrUpdateChannelCompilation).succeeded();
        assertThat(createOrUpdateChannelCompilation)
                .generatedSourceFile("event.handler.createorupdate.application.ChannelSummaryService")
                .contentsAsUtf8String()
                .contains("orElseGet(() -> channelSummaryRepository.save(ChannelSummary.onCreate(event)))");
    }

    @Test
    void pairedHandlerDoesNotGenerateSeparateServiceMethodForStaticHandler() {
        assertThat(createOrUpdateChannelCompilation).succeeded();
        assertThat(createOrUpdateChannelCompilation)
                .generatedSourceFile("event.handler.createorupdate.application.ChannelSummaryService")
                .contentsAsUtf8String()
                .doesNotContain("public void onCreate(");
    }

    @Test
    void instanceHandlerOnComponentGeneratesMethodInAggregateRootService() {
        assertThat(instanceHandlerOrderCompilation).succeeded();
        assertThat(instanceHandlerOrderCompilation)
                .generatedSourceFile("event.handler.instancehandler.application.OrderService")
                .contentsAsUtf8String()
                .contains("orderProjection.onOrderCreated(event)");
    }

    @Test
    void instanceHandlerOnComponentInjectsComponentIntoAggregateRootService() {
        assertThat(instanceHandlerOrderCompilation).succeeded();
        assertThat(instanceHandlerOrderCompilation)
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
        assertThat(multicastCreateOrUpdateChannelCompilation).succeeded();
        assertThat(multicastCreateOrUpdateChannelCompilation)
                .generatedSourceFile("event.handler.multicastcreateorupdate.application.ChannelService")
                .contentsAsUtf8String()
                .contains("public void onMessageSent(MessageEvent event)");
        assertThat(multicastCreateOrUpdateChannelCompilation)
                .generatedSourceFile("event.handler.multicastcreateorupdate.application.ChannelService")
                .contentsAsUtf8String()
                .contains("channelRepository.save(Channel.onCreate(event))");
    }

    @Test
    void multicastWithStaticCompanionDoesNotThrowWhenNoAggregatesFound() {
        assertThat(multicastCreateOrUpdateChannelCompilation).succeeded();
        assertThat(multicastCreateOrUpdateChannelCompilation)
                .generatedSourceFile("event.handler.multicastcreateorupdate.application.ChannelService")
                .contentsAsUtf8String()
                .doesNotContain("throw new IllegalStateException");
    }

    @Test
    void staticEventHandlerWithAuditFieldsPopulatesAuditOnCreate() {
        assertThat(staticHandlerAuditOrderCompilation).succeeded();
        assertThat(staticHandlerAuditOrderCompilation)
                .generatedSourceFile("event.handler.statichandleraudit.application.OrderService")
                .contentsAsUtf8String()
                .contains("var aggregate = Order.onCreate(event)");
        assertThat(staticHandlerAuditOrderCompilation)
                .generatedSourceFile("event.handler.statichandleraudit.application.OrderService")
                .contentsAsUtf8String()
                .contains("Instant.now()");
        assertThat(staticHandlerAuditOrderCompilation)
                .generatedSourceFile("event.handler.statichandleraudit.application.OrderService")
                .contentsAsUtf8String()
                .contains("auditContextProvider.currentUserId()");
    }

    @Test
    void staticEventHandlerWithAuditFieldsInjectsAuditContextProvider() {
        assertThat(staticHandlerAuditOrderCompilation).succeeded();
        assertThat(staticHandlerAuditOrderCompilation)
                .generatedSourceFile("event.handler.statichandleraudit.application.OrderService")
                .contentsAsUtf8String()
                .contains("AuditContextProvider");
    }

    @Test
    void staticEventHandlerWithOptionalReturnAndAuditFieldsPopulatesAuditOnCreate() {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("event/handler/optionalhandleraudit/source/Order.java"),
                        sourceOf("event/handler/optionalhandleraudit/source/OrderCreated.java")
                );

        assertThat(compilation).succeeded();
        assertThat(compilation)
                .generatedSourceFile("event.handler.optionalhandleraudit.application.OrderService")
                .contentsAsUtf8String()
                .contains(".map(aggregate -> new Order(");
        assertThat(compilation)
                .generatedSourceFile("event.handler.optionalhandleraudit.application.OrderService")
                .contentsAsUtf8String()
                .contains("new AuditInfo(");
        assertThat(compilation)
                .generatedSourceFile("event.handler.optionalhandleraudit.application.OrderService")
                .contentsAsUtf8String()
                .contains(".ifPresent(aggregate -> orderRepository.save(aggregate))");
    }

    @Test
    void multicastWithStaticCompanionDoesNotGenerateSeparateServiceMethodForStaticHandler() {
        assertThat(multicastCreateOrUpdateChannelCompilation).succeeded();
        assertThat(multicastCreateOrUpdateChannelCompilation)
                .generatedSourceFile("event.handler.multicastcreateorupdate.application.ChannelService")
                .contentsAsUtf8String()
                .doesNotContain("public void onCreate(");
    }
}
