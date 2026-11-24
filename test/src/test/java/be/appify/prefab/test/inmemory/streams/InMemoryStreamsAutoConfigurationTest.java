package be.appify.prefab.test.inmemory.streams;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.spring.JsonUtil;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import be.appify.prefab.streams.engine.InMemoryStreamStateRepository;
import be.appify.prefab.streams.engine.InProcessPrefabStreams;
import be.appify.prefab.test.inmemory.messaging.InMemoryEventBus;
import be.appify.prefab.test.inmemory.messaging.InMemoryMessagingAutoConfiguration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

public class InMemoryStreamsAutoConfigurationTest {

    @Event(topic = "streams-test.orders")
    public record Order(String id, String category, int amount) implements Keyed<String> {
        @Override
        public String key() {
            return id;
        }
    }

    @Event(topic = "streams-test.big-orders")
    public record BigOrder(String id, int amount) implements Keyed<String> {
        @Override
        public String key() {
            return id;
        }
    }

    @Event(topic = "streams-test.category-totals")
    public record CategoryTotal(String category, int total) implements Keyed<String> {
        static CategoryTotal empty() {
            return new CategoryTotal(null, 0);
        }

        CategoryTotal add(Order order) {
            return new CategoryTotal(order.category(), total + order.amount());
        }

        @Override
        public String key() {
            return category;
        }
    }

    @Event(topic = "streams-test.changes", typeArguments = Order.class)
    public record Changed<T>(String id, T payload) implements Keyed<String> {
        @Override
        public String key() {
            return id;
        }
    }

    @Event(topic = "streams-test.order-changes", typeArguments = Order.class)
    public record Echoed<T>(String id, T payload) implements Keyed<String> {
        @Override
        public String key() {
            return id;
        }
    }

    private static final EventType<Changed<Order>> ORDER_CHANGES = new EventType<>() {
    };
    private static final EventType<Echoed<Order>> ECHOED_ORDER_CHANGES = new EventType<>() {
    };

    @Configuration(proxyBeanMethods = false)
    static class Topologies {
        @Bean
        StreamDefinition bigOrders(PrefabStreams streams) {
            return streams.<String, Order>from(Order.class)
                    .filter(order -> order.amount() >= 100)
                    .<BigOrder>map(order -> new BigOrder(order.id(), order.amount()))
                    .to(BigOrder.class);
        }

        @Bean
        StreamDefinition categoryTotals(PrefabStreams streams) {
            return streams.<String, Order>from(Order.class)
                    .groupBy(String.class, Order::category)
                    .aggregate(CategoryTotal.class, CategoryTotal::empty, (order, total) -> total.add(order))
                    .to(CategoryTotal.class);
        }

        @Bean
        StreamDefinition namedTopic(PrefabStreams streams) {
            return streams.<String, Order>from(Order.class)
                    .filter(order -> order.amount() < 0)
                    .to("streams-test.refunds");
        }

        @Bean
        StreamDefinition genericEvents(PrefabStreams streams) {
            return streams.<String, Changed<Order>>from(ORDER_CHANGES)
                    .<Echoed<Order>>map(changed -> new Echoed<>(changed.id(), changed.payload()))
                    .to(ECHOED_ORDER_CHANGES);
        }
    }

    private static EventRegistry registry() {
        var registry = new EventRegistry();
        registry.register("streams-test.orders", Order.class, Event.Serialization.JSON);
        registry.register("streams-test.big-orders", BigOrder.class, Event.Serialization.JSON);
        registry.register("streams-test.category-totals", CategoryTotal.class, Event.Serialization.JSON);
        registry.register("streams-test.changes", ORDER_CHANGES, Event.Serialization.JSON);
        registry.register("streams-test.order-changes", ECHOED_ORDER_CHANGES, Event.Serialization.JSON);
        return registry;
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    InMemoryMessagingAutoConfiguration.class, InMemoryStreamsAutoConfiguration.class))
            .withBean(EventRegistry.class, InMemoryStreamsAutoConfigurationTest::registry)
            .withBean(ConversionService.class, DefaultConversionService::new)
            .withBean(JsonMapper.class, () -> JsonMapper.builder().build())
            .withBean(JsonUtil.class, () -> new JsonUtil(JsonMapper.builder().build()))
            .withBean(KafkaProperties.class, KafkaProperties::new)
            .withPropertyValues("prefab.test.containers=false");

    @Test
    void doesNothingWhenTestsRunWithContainers() {
        runner.withPropertyValues("prefab.test.containers=true")
                .run(context -> assertThat(context).doesNotHaveBean(InProcessPrefabStreams.class));
    }

    @Test
    void doesNotStartATopologyWithoutAnInMemoryBus() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(InMemoryStreamsAutoConfiguration.class))
                .withPropertyValues("prefab.test.containers=false")
                .run(context -> assertThat(context).doesNotHaveBean(PrefabStreams.class));
    }

    @Test
    void routesAnApplicationEventThroughATopologyToAnOutputTopic() {
        runner.withUserConfiguration(Topologies.class).run(context -> {
            var outputs = collect(context, "streams-test.big-orders");

            publish(context, new Order("o-1", "books", 150));
            publish(context, new Order("o-2", "books", 50));

            assertThat(outputs).containsExactly(new BigOrder("o-1", 150));
        });
    }

    @Test
    void keepsStateAcrossEventsAndPublishesTheUpdatedAggregate() {
        runner.withUserConfiguration(Topologies.class).run(context -> {
            var totals = collect(context, "streams-test.category-totals");

            publish(context, new Order("o-1", "books", 10));
            publish(context, new Order("o-2", "music", 7));
            publish(context, new Order("o-3", "books", 5));

            assertThat(totals).containsExactly(
                    new CategoryTotal("books", 10),
                    new CategoryTotal("music", 7),
                    new CategoryTotal("books", 15));
        });
    }

    @Test
    void publishesToATopicThatTheStreamNamesItself() {
        runner.withUserConfiguration(Topologies.class).run(context -> {
            var refunds = collect(context, "streams-test.refunds");

            publish(context, new Order("o-1", "books", -20));

            assertThat(refunds).containsExactly(new Order("o-1", "books", -20));
        });
    }

    @Test
    void carriesGenericEventsWithTheirConcreteType() {
        runner.withUserConfiguration(Topologies.class).run(context -> {
            var echoes = collect(context, "streams-test.order-changes");

            context.getBean(InMemoryEventBus.class).dispatch(
                    new be.appify.prefab.core.domain.TypedEvent<>(ORDER_CHANGES,
                            new Changed<>("c-1", new Order("o-1", "books", 1))));

            assertThat(echoes).containsExactly(new Echoed<>("c-1", new Order("o-1", "books", 1)));
        });
    }

    @Test
    void forgetsTheStateOfAggregationsWhenReset() {
        runner.withUserConfiguration(Topologies.class).run(context -> {
            var totals = collect(context, "streams-test.category-totals");
            publish(context, new Order("o-1", "books", 10));

            context.getBean(StreamStateReset.class).reset();
            publish(context, new Order("o-2", "books", 5));

            assertThat(totals).containsExactly(new CategoryTotal("books", 10), new CategoryTotal("books", 5));
        });
    }

    @Test
    void usesTheStateRepositoryOfTheApplicationWhenThereIsOne() {
        var repository = new InMemoryStreamStateRepository();
        runner.withBean(InMemoryStreamStateRepository.class, () -> repository).run(context ->
                assertThat(context.getBean(InMemoryStreamStateRepository.class)).isSameAs(repository));
    }

    private static void publish(AssertableApplicationContext context, Object event) {
        context.getBean(InMemoryEventBus.class).dispatch(event);
    }

    private static List<Object> collect(AssertableApplicationContext context, String topic) {
        var received = new ArrayList<Object>();
        context.getBean(InMemoryEventBus.class).subscribe(topic, "test",
                message -> received.add(be.appify.prefab.core.domain.TypedEvent.payload(message.decode())));
        return received;
    }
}
