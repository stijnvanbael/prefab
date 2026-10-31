package be.appify.prefab.streams.topology;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.streams.JoinWindow;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.testing.TestStreams;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestExtension;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenericEventTopologyTest {
    @RegisterExtension static final TopologyTestExtension EXTENSION = TopologyTestExtension.withTracing();
    @TestStreams PrefabStreams streams;
    @TestStreams TopologySession topology;

    @Event(topic = "shared", typeArguments = String.class)
    @Event(topic = "shared", typeArguments = Integer.class)
    public record Changed<T>(String id, T payload) implements Keyed<String> {
        public String key() { return id; }
    }
    @Event(topic = "strings-out", typeArguments = String.class)
    @Event(topic = "integers-out", typeArguments = Integer.class)
    public record Result<T>(String id, T payload) implements Keyed<String> {
        public String key() { return id; }
    }
    private static final EventType<Changed<String>> STRINGS = new EventType<>() {};
    private static final EventType<Changed<Integer>> INTEGERS = new EventType<>() {};
    private static final EventType<Result<String>> STRING_RESULT = new EventType<>() {};
    private static final EventType<Result<Integer>> INTEGER_RESULT = new EventType<>() {};

    @Test
    void sharedTopicIsolatesBindingsAndPreservesTracingThroughRepartition() {
        streams.from(STRINGS).filter(event -> !event.payload().isEmpty())
                .map(event -> new Result<>(event.id(), event.payload().toUpperCase()))
                .as(STRING_RESULT).repartition().to(STRING_RESULT);
        streams.from(INTEGERS).map(event -> new Result<>(event.id(), event.payload() + 1))
                .to(INTEGER_RESULT);
        topology.input(STRINGS).sendWithMessageId(new Changed<>("a", "hello"), "m-string");
        topology.input(INTEGERS).sendWithMessageId(new Changed<>("b", 2), "m-integer");
        var strings = topology.output(STRING_RESULT).messages();
        assertThat(strings).hasSize(1);
        assertThat(strings.getFirst().value()).isEqualTo(new Result<>("a", "HELLO"));
        assertThat(strings.getFirst().correlationIds()).contains("m-string").doesNotContain("m-integer");
        assertThat(topology.output(INTEGER_RESULT).values()).containsExactly(new Result<>("b", 3));
    }

    @Test
    void joinStoresRetainBothConcreteValueTypes() {
        streams.from(STRINGS).join(streams.from(INTEGERS),
                JoinWindow.of(Duration.ofSeconds(10), Duration.ofSeconds(1)),
                (text, number) -> new Result<>(text.id(), text.payload() + number.payload()))
                .as(STRING_RESULT).to(STRING_RESULT);
        topology.input(STRINGS).sendWithMessageId(new Changed<>("a", "value"), "text");
        topology.input(INTEGERS).sendWithMessageId(new Changed<>("a", 7), "number");
        var result = topology.output(STRING_RESULT).messages().getFirst();
        assertThat(result.value()).isEqualTo(new Result<>("a", "value7"));
        assertThat(result.correlationIds()).containsExactlyInAnyOrder("text", "number");
    }

    @Test
    void aggregationStateRetainsFullResultTypeAcrossUpdates() {
        streams.from(INTEGERS).groupBy(String.class, Changed::id)
                .aggregate(INTEGER_RESULT, () -> new Result<>("", 0),
                        (value, state) -> new Result<>(value.id(), state.payload() + value.payload()))
                .to(INTEGER_RESULT);
        var input = topology.input(INTEGERS);
        input.send(new Changed<>("a", 2));
        input.send(new Changed<>("a", 3));
        assertThat(topology.output(INTEGER_RESULT).values())
                .containsExactly(new Result<>("a", 2), new Result<>("a", 5));
    }

    @Test
    void listAggregationProcessorRetainsInputArguments() {
        streams.from(STRINGS).aggregate(Changed::id,
                values -> new Result<>(values.getFirst().id(), values.stream().map(Changed::payload)
                        .collect(java.util.stream.Collectors.joining(","))),
                result -> true).as(STRING_RESULT).to(STRING_RESULT);
        topology.input(STRINGS).send(new Changed<>("a", "one"));
        topology.input(STRINGS).send(new Changed<>("a", "two"));
        assertThat(topology.output(STRING_RESULT).values())
                .containsExactly(new Result<>("a", "one"), new Result<>("a", "one,two"));
    }

    @Test
    void globalStoreRetainsBindingAndSkipsOtherSharedTopicValues() {
        var store = streams.globalStore(STRINGS);
        assertThatThrownBy(() -> streams.globalStore(INTEGERS)).hasMessageContaining("already feeds global store");
        topology.input(STRINGS).send(new Changed<>("a", "kept"));
        topology.input(INTEGERS).send(new Changed<>("a", 9));
        assertThat(topology.lookup(store, "a")).contains(new Changed<>("a", "kept"));
        topology.input(STRINGS).sendTombstone("a");
        assertThat(topology.lookup(store, "a")).isEmpty();
    }

    @Test
    void typedKeyByPreservesMappedArguments() {
        streams.from(STRINGS).keyBy(String.class, STRING_RESULT,
                event -> new Result<>("new-" + event.id(), event.payload())).to(STRING_RESULT);
        topology.input(STRINGS).send(new Changed<>("a", "value"));
        assertThat(topology.output(STRING_RESULT).values()).containsExactly(new Result<>("new-a", "value"));
    }
}
