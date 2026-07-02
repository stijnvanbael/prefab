package be.appify.prefab.processor.pubsub;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static be.appify.prefab.processor.pubsub.ProcessorTestUtil.generatedSourceOf;
import static be.appify.prefab.processor.pubsub.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

class PubSubEventTypeRegistrarWriterTest {

    @Test
    void singleEventType() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("pubsub/single/User.java"),
                        sourceOf("pubsub/single/UserCreated.java"),
                        sourceOf("pubsub/single/UserExporter.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "pubsub.single.infrastructure.event.UserCreatedEventTypeRegistrar");
        assertThat(source).contains("@Component(\"pubsub_single_UserCreatedEventTypeRegistrar\")");
        assertThat(source).contains("implements EventRegistryCustomizer");
        assertThat(source).contains("registry.register(\"user\", UserCreated.class, Event.Serialization.JSON");
    }

    @Test
    void publishToAll() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("pubsub/publishtoall/UserEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "pubsub.publishtoall.infrastructure.event.UserEventEventTypeRegistrar");
        assertThat(source).contains("@Component(\"pubsub_publishtoall_UserEventEventTypeRegistrar\")");
        assertThat(source).contains("registry.register(");
        assertThat(source).contains("registry.registerPublishTo(UserEvent.class, PublishTo.ALL)");
    }
}
