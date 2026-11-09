package be.appify.prefab.processor.pubsub;

import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;

class GenericPubSubBindingTest {
    @ParameterizedTest
    @ValueSource(strings = {"shared", "${topics.changes:shared}", "separate"})
    void generatesIndependentConcreteSubscriptionsOnTheSameTopic(String topic) throws Exception {
        var compilation = javac().withProcessors(new PrefabProcessor()).compile(
                JavaFileObjects.forSourceString("example.Changed", """
                        package example;
                        import be.appify.prefab.core.annotations.Event;
                        @Event(topic = "%s", typeArguments = String.class, platform = Event.Platform.PUB_SUB)
                        @Event(topic = "%s", typeArguments = Integer.class, platform = Event.Platform.PUB_SUB)
                        public record Changed<T>(String id, T payload) {}
                        """.formatted(topic, topic.equals("separate") ? "numbers" : topic)),
                JavaFileObjects.forSourceString("example.Handlers", """
                        package example;
                        import be.appify.prefab.core.annotations.EventHandler;
                        import org.springframework.stereotype.Component;
                        @Component
                        public class Handlers {
                            @EventHandler public void strings(Changed<String> event) {}
                            @EventHandler public void numbers(Changed<Integer> event) {}
                        }
                        """));

        assertThat(compilation).succeeded();
        var source = compilation.generatedSourceFile("example.infrastructure.pubsub.HandlersPubSubSubscriber")
                .orElseThrow().getCharContent(false).toString();
        assertThat(source).contains("new EventType<Changed<String>>()", "new EventType<Changed<Integer>>()",
                "SubscriptionRequest<Changed<String>>", "SubscriptionRequest<Changed<Integer>>",
                "EventSubscriptionNames.name(", "handlers.strings(event)", "handlers.numbers(event)")
                .doesNotContain("Changed<String>.class", "Changed<Integer>.class");
        assertThat(source.split("pubSub.subscribe\\(", -1)).hasSize(3);
    }
}
