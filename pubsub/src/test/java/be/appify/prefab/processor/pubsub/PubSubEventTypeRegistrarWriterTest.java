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
    void publishToAll() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("pubsub/publishtoall/UserEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "pubsub.publishtoall.infrastructure.pubsub.UserEventPubSubEventTypeRegistrar",
                "expected/pubsub/publishtoall/UserEventPubSubEventTypeRegistrar.java");
    }
}

