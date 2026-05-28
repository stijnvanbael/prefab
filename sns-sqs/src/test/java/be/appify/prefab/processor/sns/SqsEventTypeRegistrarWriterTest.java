package be.appify.prefab.processor.sns;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static be.appify.prefab.processor.sns.ProcessorTestUtil.assertGeneratedSourceEqualsIgnoringWhitespace;
import static be.appify.prefab.processor.sns.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class SqsEventTypeRegistrarWriterTest {

    @Test
    void singleEventType() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("sns/single/User.java"),
                        sourceOf("sns/single/UserCreated.java"),
                        sourceOf("sns/single/UserExporter.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "sns.single.infrastructure.event.UserCreatedEventTypeRegistrar",
                "expected/sns/single/UserCreatedEventTypeRegistrar.java");
    }

    @Test
    void supertypeEventType() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("sns/supertype/UserEvent.java"),
                        sourceOf("sns/supertype/UserCreated.java"),
                        sourceOf("sns/supertype/UserUpdated.java"),
                        sourceOf("sns/supertype/UserExporter.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "sns.supertype.infrastructure.event.UserEventEventTypeRegistrar",
                "expected/sns/supertype/UserEventEventTypeRegistrar.java");
    }

    @Test
    void publishToAll() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("sns/publishtoall/UserEvent.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "sns.publishtoall.infrastructure.event.UserEventEventTypeRegistrar",
                "expected/sns/publishtoall/UserEventEventTypeRegistrar.java");
    }
}
