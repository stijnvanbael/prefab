package be.appify.prefab.processor.pubsub;

import org.junit.jupiter.api.Test;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.JavaFileObjects;
import org.springframework.core.io.ClassPathResource;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class PubSubPublisherWriterTest {
    @Test
    void singleEventType() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("pubsub/single/User.java"),
                        sourceOf("pubsub/single/UserCreated.java"),
                        sourceOf("pubsub/single/UserEventHandler.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("pubsub.single.infrastructure.pubsub.UserCreatedPubSubPublisher")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/pubsub/single/UserCreatedPubSubPublisher.java"));
    }

    @Test
    void multipleEventTypes() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("pubsub/multiple/User.java"),
                        sourceOf("pubsub/multiple/UserEvent.java"),
                        sourceOf("pubsub/multiple/UserEventHandler.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("pubsub.multiple.infrastructure.pubsub.UserEventPubSubPublisher")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/pubsub/multiple/UserEventPubSubPublisher.java"));
    }

    private String contentsOf(String fileName) throws IOException {
        return new ClassPathResource(fileName).getContentAsString(StandardCharsets.UTF_8);
    }

    private JavaFileObject sourceOf(String name) throws IOException {
        var resource = new ClassPathResource(name).getURL();
        return JavaFileObjects.forResource(resource);
    }
}
