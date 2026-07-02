package be.appify.prefab.processor.sns;

import be.appify.prefab.processor.PrefabProcessor;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static be.appify.prefab.processor.sns.ProcessorTestUtil.generatedSourceOf;
import static be.appify.prefab.processor.sns.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

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
        var source = generatedSourceOf(compilation, "sns.single.infrastructure.event.UserCreatedEventTypeRegistrar");
        assertThat(source).contains("@Component(\"sns_single_UserCreatedEventTypeRegistrar\")");
        assertThat(source).contains("implements EventRegistryCustomizer");
        assertThat(source).contains("registry.register(\"user\", UserCreated.class, Event.Serialization.JSON");
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
        var source = generatedSourceOf(compilation, "sns.supertype.infrastructure.event.UserEventEventTypeRegistrar");
        assertThat(source).contains("@Component(\"sns_supertype_UserEventEventTypeRegistrar\")");
        assertThat(source).contains("@Value(\"${topic.user.name}\")");
        assertThat(source).contains("registry.register(userEventTopic, UserEvent.class, Event.Serialization.JSON");
    }

    @Test
    void publishToAll() throws IOException {
        var compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(sourceOf("sns/publishtoall/UserEvent.java"));
        assertThat(compilation).succeeded();
        var source = generatedSourceOf(compilation, "sns.publishtoall.infrastructure.event.UserEventEventTypeRegistrar");
        assertThat(source).contains("@Component(\"sns_publishtoall_UserEventEventTypeRegistrar\")");
        assertThat(source).contains("registry.register(");
        assertThat(source).contains("registry.registerPublishTo(UserEvent.class, PublishTo.ALL)");
    }
}
