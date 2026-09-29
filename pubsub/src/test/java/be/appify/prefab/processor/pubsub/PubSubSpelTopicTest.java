package be.appify.prefab.processor.pubsub;

import be.appify.prefab.core.pubsub.PubSubUtil;
import be.appify.prefab.core.pubsub.SubscriptionRequest;
import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.Compilation;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import static be.appify.prefab.processor.pubsub.ProcessorTestUtil.classLoaderOf;
import static be.appify.prefab.processor.pubsub.ProcessorTestUtil.generatedSourceOf;
import static be.appify.prefab.processor.pubsub.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PubSubSpelTopicTest {
    private static final String SUBSCRIBER = "pubsub.speltopic.infrastructure.pubsub.UserServicePubSubSubscriber";

    private Compilation compilation;

    @BeforeEach
    void compile() throws IOException {
        compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("pubsub/speltopic/UserEvent.java"),
                        sourceOf("pubsub/speltopic/UserService.java"));
        assertThat(compilation).succeeded();
    }

    @Test
    void spelTopicExpressionIsInjectedInsteadOfUsedLiterally() {
        var source = generatedSourceOf(compilation, SUBSCRIBER);
        assertThat(source).contains("@Value(\"#{'${topic.user.names}'.split(',')}\") String[] userEvent0Topics");
        assertThat(source).contains("@Value(\"#{'${topic.prefix}' + '.dlt'}\") String deadLetterTopic");
        assertThat(source).contains("for (var topic : userEvent0Topics)");
        assertThat(source).contains("SubscriptionRequest<UserEvent>(topic, \"user-service-on-user-event-\" + topic");
        assertThat(source).contains(
                "SubscriptionRequest<UserEvent>(\"prefab.user.audit\", \"user-service-on-user-event-prefab.user.audit\"");
        assertThat(source).doesNotContain("SubscriptionRequest<UserEvent>(\"#{");
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void spelTopicExpressionExpandingToMultipleTopicsSubscribesToEachResolvedTopicWithItsOwnSubscription() throws Exception {
        var classLoader = classLoaderOf(compilation);
        var pubSub = mock(PubSubUtil.class);
        try (var applicationContext = new AnnotationConfigApplicationContext()) {
            applicationContext.setClassLoader(classLoader);
            applicationContext.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                    "topic.user.names", "prefab.user.created,prefab.user.updated",
                    "topic.prefix", "prefab")));
            applicationContext.registerBean(PubSubUtil.class, () -> pubSub);
            applicationContext.registerBean(classLoader.loadClass("pubsub.speltopic.UserService"));
            applicationContext.registerBean(classLoader.loadClass(SUBSCRIBER));
            applicationContext.refresh();
        }

        var requests = ArgumentCaptor.forClass(SubscriptionRequest.class);
        verify(pubSub, times(3)).subscribe(requests.capture());
        assertThat(requests.getAllValues())
                .extracting(request -> ((SubscriptionRequest<?>) request).topic())
                .containsExactly("prefab.user.created", "prefab.user.updated", "prefab.user.audit");
        assertThat(requests.getAllValues())
                .extracting(request -> ((SubscriptionRequest<?>) request).subscription())
                .containsExactly(
                        "user-service-on-user-event-prefab.user.created",
                        "user-service-on-user-event-prefab.user.updated",
                        "user-service-on-user-event-prefab.user.audit");
        assertThat(requests.getAllValues())
                .extracting(request -> ((SubscriptionRequest<?>) request).deadLetterPolicy().getDeadLetterTopic())
                .containsOnly("prefab.dlt");
    }
}
