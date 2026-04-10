package be.appify.prefab.processor.event.handler;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.event.avro.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class EventHandlerWriterTest {

    @Test
    void staticEventHandlerGeneratesServiceMethod() throws IOException {
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
    void staticEventHandlerAddsEventListenerAnnotation() throws IOException {
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
    void staticEventHandlerWithOptionalReturnUsesIfPresent() throws IOException {
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
    void staticEventHandlerWithPlatformEventOmitsEventListenerAnnotation() throws IOException {
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
    void multicastEventHandlerQueriesRepository() throws IOException {
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
    void multicastEventHandlerThrowsWhenNoAggregatesFound() throws IOException {
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
    void multicastEventHandlerSavesAllAggregates() throws IOException {
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
    void byReferenceEventHandlerLoadsAggregateByReference() throws IOException {
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
    void byReferenceEventHandlerSavesAggregateAfterHandling() throws IOException {
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
    void byReferenceEventHandlerThrowsWhenAggregateNotFound() throws IOException {
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
}
