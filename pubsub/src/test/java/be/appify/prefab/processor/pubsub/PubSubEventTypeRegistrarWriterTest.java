package be.appify.prefab.processor.pubsub;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static be.appify.prefab.processor.pubsub.ProcessorTestUtil.assertGeneratedSourceEqualsIgnoringWhitespace;
import static be.appify.prefab.processor.pubsub.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

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
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "pubsub.single.infrastructure.event.UserCreatedEventTypeRegistrar",
                "expected/pubsub/single/UserCreatedEventTypeRegistrar.java");
    }

    @Test
    void publishToAll() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("pubsub/publishtoall/UserEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "pubsub.publishtoall.infrastructure.event.UserEventEventTypeRegistrar",
                "expected/pubsub/publishtoall/UserEventEventTypeRegistrar.java");
    }
}
