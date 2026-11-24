package be.appify.prefab.test.inmemory.messaging;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.spring.JsonUtil;
import be.appify.prefab.test.EventConsumer;
import be.appify.prefab.test.TestEventConsumer;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.test.context.TestContext;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class InMemoryTestListenersTest {

    @Event(topic = "users")
    public record UserCreated(String id) {
    }

    static class Fixture {
        @TestEventConsumer(topic = "${topics.users}")
        EventConsumer<UserCreated> consumer;
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(InMemoryMessagingAutoConfiguration.class))
            .withBean(EventRegistry.class, () -> {
                var registry = new EventRegistry();
                registry.register("users", UserCreated.class, Event.Serialization.JSON);
                return registry;
            })
            .withBean(ConversionService.class, DefaultConversionService::new)
            .withBean(JsonMapper.class, () -> JsonMapper.builder().build())
            .withBean(JsonUtil.class, () -> new JsonUtil(JsonMapper.builder().build()))
            .withPropertyValues("topics.users=users");

    private static TestContext testContext(ApplicationContext context, Fixture instance) {
        var attributes = new HashMap<String, Object>();
        var testContext = mock(TestContext.class);
        doReturn(context).when(testContext).getApplicationContext();
        doReturn(Fixture.class).when(testContext).getTestClass();
        doReturn(instance).when(testContext).getTestInstance();
        doAnswer(invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(testContext).setAttribute(anyString(), any());
        doAnswer(invocation -> attributes.remove(invocation.<String>getArgument(0)))
                .when(testContext).removeAttribute(anyString());
        return testContext;
    }

    @Test
    void injectsEventConsumersThatReceiveDecodedEventsAndAreResetPerTest() {
        runner.withPropertyValues("prefab.test.containers=false").run(context -> {
            var listener = new InMemoryEventConsumerExecutionListener();
            var bus = context.getBean(InMemoryEventBus.class);
            var first = new Fixture();
            var firstTest = testContext(context, first);

            listener.beforeTestMethod(firstTest);
            bus.dispatch(new UserCreated("1"));
            listener.afterTestMethod(firstTest);
            bus.dispatch(new UserCreated("ignored"));

            assertThat(first.consumer.messages()).containsExactly(new UserCreated("1"));

            var second = new Fixture();
            var secondTest = testContext(context, second);
            listener.beforeTestMethod(secondTest);
            bus.dispatch(new UserCreated("2"));
            listener.afterTestMethod(secondTest);

            assertThat(second.consumer.messages()).containsExactly(new UserCreated("2"));
            assertThat(first.consumer.messages()).containsExactly(new UserCreated("1"));
        });
    }

    @Test
    void leavesEventConsumersAloneInContainersMode() {
        runner.withPropertyValues("prefab.test.containers=true").run(context -> {
            var fixture = new Fixture();

            new InMemoryEventConsumerExecutionListener().beforeTestMethod(testContext(context, fixture));

            assertThat(fixture.consumer).isNull();
        });
    }

    @Test
    void failsTheTestWhenAHandlerFailedDuringIt() {
        runner.withPropertyValues("prefab.test.containers=false").run(context -> {
            var listener = new InMemoryBusTestExecutionListener();
            var bus = context.getBean(InMemoryEventBus.class);
            var test = testContext(context, new Fixture());
            var first = new IllegalStateException("first");
            var second = new IllegalArgumentException("second");
            listener.beforeTestMethod(test);
            bus.subscribe("users", "broken", message -> {
                throw first;
            });
            bus.subscribe("users", "worse", message -> {
                throw second;
            });

            bus.dispatch(new UserCreated("1"));

            assertThatThrownBy(() -> listener.afterTestMethod(test))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("2 event handler(s) failed")
                    .hasCause(first)
                    .hasSuppressedException(second);
            assertThat(bus.failures()).isEmpty();
        });
    }

    @Test
    void doesNotFailTestsWithoutHandlerFailuresAndForgetsEarlierOnes() {
        runner.withPropertyValues("prefab.test.containers=false").run(context -> {
            var listener = new InMemoryBusTestExecutionListener();
            var bus = context.getBean(InMemoryEventBus.class);
            var test = testContext(context, new Fixture());
            bus.subscribe("users", "broken", message -> {
                throw new IllegalStateException("from an earlier test");
            });
            bus.dispatch(new UserCreated("1"));

            listener.beforeTestMethod(test);
            listener.afterTestMethod(test);

            assertThat(bus.failures()).isEmpty();
        });
    }
}
