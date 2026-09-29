package be.appify.prefab.processor.sns;

import be.appify.prefab.core.sns.SqsSubscriptionRequest;
import be.appify.prefab.core.sns.SqsUtil;
import be.appify.prefab.processor.PrefabProcessor;
import com.google.testing.compile.Compilation;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import static be.appify.prefab.processor.sns.ProcessorTestUtil.classLoaderOf;
import static be.appify.prefab.processor.sns.ProcessorTestUtil.generatedSourceOf;
import static be.appify.prefab.processor.sns.ProcessorTestUtil.sourceOf;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SqsSpelTopicTest {
    private static final String SUBSCRIBER = "sns.speltopic.infrastructure.sns.UserServiceSqsSubscriber";

    private Compilation compilation;

    @BeforeEach
    void compile() throws IOException {
        compilation = javac()
                .withProcessors(new PrefabProcessor())
                .compile(
                        sourceOf("sns/speltopic/UserEvent.java"),
                        sourceOf("sns/speltopic/UserService.java"));
        assertThat(compilation).succeeded();
    }

    @Test
    void spelTopicExpressionIsInjectedInsteadOfUsedLiterally() {
        var source = generatedSourceOf(compilation, SUBSCRIBER);
        assertThat(source).contains("@Value(\"#{'${topic.user.names}'.split(',')}\") String[] userEvent0Topics");
        assertThat(source).contains("@Value(\"#{'${topic.prefix}' + '.dlt'}\") String deadLetterTopic");
        assertThat(source).contains("for (var topic : userEvent0Topics)");
        assertThat(source).contains("SqsSubscriptionRequest<UserEvent>(\"prefab.user.audit\"");
        assertThat(source).doesNotContain("SqsSubscriptionRequest<UserEvent>(\"#{");
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void spelTopicExpressionExpandingToMultipleTopicsSubscribesToEachResolvedTopic() throws Exception {
        var classLoader = classLoaderOf(compilation);
        var sqsUtil = mock(SqsUtil.class);
        try (var applicationContext = new AnnotationConfigApplicationContext()) {
            applicationContext.setClassLoader(classLoader);
            applicationContext.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                    "topic.user.names", "prefab.user.created,prefab.user.updated",
                    "topic.prefix", "prefab")));
            applicationContext.registerBean(SqsUtil.class, () -> sqsUtil);
            applicationContext.registerBean(classLoader.loadClass("sns.speltopic.UserService"));
            applicationContext.registerBean(classLoader.loadClass(SUBSCRIBER));
            applicationContext.refresh();
        }

        var requests = ArgumentCaptor.forClass(SqsSubscriptionRequest.class);
        verify(sqsUtil, times(3)).subscribe(requests.capture());
        assertThat(requests.getAllValues())
                .extracting(request -> ((SqsSubscriptionRequest<?>) request).topic())
                .containsExactly("prefab.user.created", "prefab.user.updated", "prefab.user.audit");
        assertThat(requests.getAllValues())
                .extracting(request -> ((SqsSubscriptionRequest<?>) request).deadLetterQueueName())
                .containsOnly("prefab.dlt");
    }
}
