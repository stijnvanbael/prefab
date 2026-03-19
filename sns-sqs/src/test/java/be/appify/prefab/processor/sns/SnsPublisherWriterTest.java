package be.appify.prefab.processor.sns;

import be.appify.prefab.processor.PrefabProcessor;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static be.appify.prefab.processor.sns.ProcessorTestUtil.contentsOf;
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
        assertThat(compilation).generatedSourceFile("sns.single.infrastructure.sns.UserCreatedSnsPublisher")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/sns/single/UserCreatedSnsPublisher.java"));
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
        assertThat(compilation).generatedSourceFile("sns.multiple.infrastructure.sns.UserEventSnsPublisher")
                .contentsAsUtf8String()
                .isEqualTo(contentsOf("expected/sns/multiple/UserEventSnsPublisher.java"));
    }
}
