package be.appify.prefab.processor.kafka;

import org.junit.jupiter.api.Test;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.JavaFileObjects;
import org.springframework.core.io.ClassPathResource;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class KafkaProducerWriterTest {
    @Test
    void singleEventType() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/single/User.java"),
                        sourceOf("kafka/single/UserCreated.java"),
                        sourceOf("kafka/single/UserEventHandler.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("kafka.single.infrastructure.kafka.UserCreatedKafkaProducer")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/kafka/single/UserCreatedKafkaProducer.java"));
    }

    @Test
    void multipleEventTypes() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("kafka/multiple/User.java"),
                        sourceOf("kafka/multiple/UserEvent.java"),
                        sourceOf("kafka/multiple/UserEventHandler.java"));
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile("kafka.multiple.infrastructure.kafka.UserEventKafkaProducer")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/kafka/multiple/UserEventKafkaProducer.java"));
    }

    private String contentsOf(String fileName) throws IOException {
        return new ClassPathResource(fileName).getContentAsString(StandardCharsets.UTF_8);
    }

    private JavaFileObject sourceOf(String name) throws IOException {
        var resource = new ClassPathResource(name).getURL();
        return JavaFileObjects.forResource(resource);
    }
}
