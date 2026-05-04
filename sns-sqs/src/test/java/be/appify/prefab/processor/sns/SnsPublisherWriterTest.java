package be.appify.prefab.processor.sns;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.sns.ProcessorTestUtil.assertGeneratedSourceEqualsIgnoringWhitespace;
import static be.appify.prefab.processor.sns.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

class SnsPublisherWriterTest {
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
                "sns.single.infrastructure.sns.UserCreatedSnsPublisher",
                "expected/sns/single/UserCreatedSnsPublisher.java");
    }

    @Test
    void multipleEventTypes() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("sns/multiple/User.java"),
                        sourceOf("sns/multiple/UserEvent.java"),
                        sourceOf("sns/multiple/UserExporter.java"));
        assertThat(compilation).succeeded();
        assertGeneratedSourceEqualsIgnoringWhitespace(
                compilation,
                "sns.multiple.infrastructure.sns.UserEventSnsPublisher",
                "expected/sns/multiple/UserEventSnsPublisher.java");
    }
}
