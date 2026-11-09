package be.appify.prefab.core.kafka;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PublishTo;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.TypedEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenericEventRegistryTest {
    record Changed<T>(String id, T payload) {}

    private final EventType<Changed<String>> strings = new EventType<>() {};
    private final EventType<Changed<Integer>> integers = new EventType<>() {};
    private final EventRegistry registry = new EventRegistry();

    @Test
    void isolatesBindingsAndAppliesFanOutWithinOneBinding() {
        registry.register("strings", strings, Event.Serialization.JSON, Changed::id);
        registry.register("strings-audit", strings, Event.Serialization.JSON, Changed::id);
        registry.register("integers", integers, Event.Serialization.JSON, Changed::id);
        var event = new TypedEvent<>(strings, new Changed<>("key", "hello"));

        assertThat(registry.topicsForDispatch(event)).containsExactly("strings");
        registry.registerPublishTo(strings, PublishTo.ALL);
        assertThat(registry.topicsForDispatch(event)).containsExactly("strings", "strings-audit");
        assertThat(registry.topicForType(integers)).isEqualTo("integers");
        assertThat(registry.keyFor(event)).contains("key");
        assertThat(registry.hasTopicForType(Changed.class)).isTrue();
    }

    @Test
    void resolvesOnlyRegisteredIdentitiesOnTheirConfiguredTopics() {
        registry.register("strings", strings, Event.Serialization.JSON);
        registry.register("integers", integers, Event.Serialization.JSON);

        assertThat(registry.concreteTypeFor("strings", strings.id())).isEqualTo(strings);
        assertThatThrownBy(() -> registry.concreteTypeFor("strings", integers.id()))
                .isInstanceOf(UnknownEventTypeException.class);
        assertThatThrownBy(() -> registry.topicsForDispatch(new Changed<>("key", "hello")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("explicit EventType");
    }
}
