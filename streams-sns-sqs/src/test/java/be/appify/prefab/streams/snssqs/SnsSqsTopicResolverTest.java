package be.appify.prefab.streams.snssqs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.streams.snssqs.StreamFixtures.CategoryTotal;
import be.appify.prefab.streams.snssqs.StreamFixtures.Sale;
import org.junit.jupiter.api.Test;

class SnsSqsTopicResolverTest {
    private final EventRegistry registry = new EventRegistry();
    private final SnsSqsTopicResolver resolver = new SnsSqsTopicResolver(registry);

    @Test
    void resolvesTheTopicRegisteredForAnEvent() {
        registry.register("sales", Sale.class, Event.Serialization.JSON);

        assertThat(resolver.topicFor(Sale.class)).isEqualTo("sales");
    }

    @Test
    void failsForAnUnregisteredType() {
        assertThatThrownBy(() -> resolver.topicFor(CategoryTotal.class)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aTopicWrittenByNameCanBeReadBack() {
        resolver.register(CategoryTotal.class, "category-totals");

        assertThat(resolver.topicFor(CategoryTotal.class)).isEqualTo("category-totals");
    }

    @Test
    void anAliasTakesPrecedenceOverTheRegistry() {
        registry.register("sales", Sale.class, Event.Serialization.JSON);
        resolver.register(Sale.class, "renamed-sales");

        assertThat(resolver.topicFor(Sale.class)).isEqualTo("renamed-sales");
    }
}
