package be.appify.prefab.test.domain;

import be.appify.prefab.core.domain.DomainEventPublisher;
import be.appify.prefab.core.domain.PublishesEvents;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.TestContext;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DomainEventPublisherTestExecutionListenerTest {

    private final DomainEventPublisherTestExecutionListener listener = new DomainEventPublisherTestExecutionListener();

    @AfterEach
    void tearDown() {
        DomainEventPublisher.reset();
    }

    @Test
    void beforeTestMethod_withTwoContexts_bindsPublisherOfTheTestsOwnContext() throws Exception {
        try (var first = contextWithPublisher(); var second = contextWithPublisher()) {
            var firstPublisher = first.getBean(CountingPublisher.class);
            var secondPublisher = second.getBean(CountingPublisher.class);

            var firstTest = testContext(first);
            listener.beforeTestMethod(firstTest);
            new PublishingAggregate().fire();
            listener.afterTestMethod(firstTest);

            var secondTest = testContext(second);
            listener.beforeTestMethod(secondTest);
            new PublishingAggregate().fire();
            new PublishingAggregate().fire();
            listener.afterTestMethod(secondTest);

            assertThat(firstPublisher.count.get()).isEqualTo(1);
            assertThat(secondPublisher.count.get()).isEqualTo(2);
        }
    }

    @Test
    void afterTestMethod_restoresPreviouslyBoundPublisher() {
        var previous = new CountingPublisher();
        DomainEventPublisher.setInstance(previous);
        try (var context = contextWithPublisher()) {
            var test = testContext(context);

            listener.beforeTestMethod(test);
            assertThat(DomainEventPublisher.getInstance()).isSameAs(context.getBean(CountingPublisher.class));
            listener.afterTestMethod(test);

            assertThat(DomainEventPublisher.getInstance()).isSameAs(previous);
        }
    }

    @Test
    void afterTestMethod_withNoPreviousPublisher_resetsInstance() {
        try (var context = contextWithPublisher()) {
            var test = testContext(context);

            listener.beforeTestMethod(test);
            listener.afterTestMethod(test);

            assertThat(DomainEventPublisher.getInstance()).isNull();
        }
    }

    @Test
    void beforeTestMethod_withoutPublisherBean_leavesInstanceUntouched() {
        var existing = new CountingPublisher();
        DomainEventPublisher.setInstance(existing);
        try (var context = new AnnotationConfigApplicationContext()) {
            context.refresh();
            var test = testContext(context);

            listener.beforeTestMethod(test);
            listener.afterTestMethod(test);

            assertThat(DomainEventPublisher.getInstance()).isSameAs(existing);
        }
    }

    private static AnnotationConfigApplicationContext contextWithPublisher() {
        var context = new AnnotationConfigApplicationContext();
        context.registerBean(CountingPublisher.class);
        context.refresh();
        return context;
    }

    private static TestContext testContext(AnnotationConfigApplicationContext context) {
        var attributes = new ConcurrentHashMap<String, Object>();
        var testContext = mock(TestContext.class);
        when(testContext.getApplicationContext()).thenReturn(context);
        doAnswer(i -> attributes.put(i.getArgument(0), i.getArgument(1)))
                .when(testContext).setAttribute(anyString(), any());
        when(testContext.hasAttribute(anyString()))
                .thenAnswer(i -> attributes.containsKey(i.<String>getArgument(0)));
        when(testContext.removeAttribute(anyString()))
                .thenAnswer(i -> attributes.remove(i.<String>getArgument(0)));
        return testContext;
    }

    record Fired() {}

    record PublishingAggregate() implements PublishesEvents {
        void fire() {
            publish(new Fired());
        }
    }

    static class CountingPublisher extends DomainEventPublisher {
        final AtomicInteger count = new AtomicInteger();

        @Override
        public void publish(Object event) {
            count.incrementAndGet();
        }
    }
}
