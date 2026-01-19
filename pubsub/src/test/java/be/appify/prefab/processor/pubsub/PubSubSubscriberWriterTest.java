package be.appify.prefab.processor.pubsub;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.pubsub.ProcessorTestUtil.contentsOf;
import static be.appify.prefab.processor.pubsub.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class PubSubSubscriberWriterTest {
    @Test
    void singleEventType() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("pubsub/single/User.java"),
                        sourceOf("pubsub/single/UserCreated.java"),
                        sourceOf("pubsub/single/UserExporter.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("pubsub.single.infrastructure.pubsub.UserExporterPubSubSubscriber")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/pubsub/single/UserExporterPubSubSubscriber.java"));
    }

    @Test
    void multipleEventTypes() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("pubsub/multiple/User.java"),
                        sourceOf("pubsub/multiple/UserEvent.java"),
                        sourceOf("pubsub/multiple/UserExporter.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile(
                        "pubsub.multiple.infrastructure.pubsub.UserExporterPubSubSubscriber")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/pubsub/multiple/UserExporterPubSubSubscriber.java"));
    }

    @Test
    void noParentEventType() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("pubsub/noparent/User.java"),
                        sourceOf("pubsub/noparent/UserEvent.java"),
                        sourceOf("pubsub/noparent/UserExporter.java"));
        assertThat(compilation).hadErrorContaining("share the same topic [user] but have no common ancestor");
    }

    @Test
    void multipleTopics() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("pubsub/multitopic/Sale.java"),
                        sourceOf("pubsub/multitopic/Refund.java"),
                        sourceOf("pubsub/multitopic/DayTotal.java"),
                        sourceOf("pubsub/multitopic/DayTotalRepositoryMixin.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("pubsub.multitopic.infrastructure.pubsub.DayTotalPubSubSubscriber")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/pubsub/multitopic/DayTotalPubSubSubscriber.java"));
    }

    @Test
    void customDlt() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("pubsub/customdlt/User.java"),
                        sourceOf("pubsub/customdlt/UserEvent.java"),
                        sourceOf("pubsub/customdlt/UserExporter.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile(
                        "pubsub.customdlt.infrastructure.pubsub.UserExporterPubSubSubscriber")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/pubsub/customdlt/UserExporterPubSubSubscriber.java"));
    }

    @Test
    void dltDisabled() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("pubsub/dltdisabled/User.java"),
                        sourceOf("pubsub/dltdisabled/UserEvent.java"),
                        sourceOf("pubsub/dltdisabled/UserExporter.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile(
                        "pubsub.dltdisabled.infrastructure.pubsub.UserExporterPubSubSubscriber")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/pubsub/dltdisabled/UserExporterPubSubSubscriber.java"));
    }
}
