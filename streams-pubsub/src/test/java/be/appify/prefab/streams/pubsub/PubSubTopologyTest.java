package be.appify.prefab.streams.pubsub;

import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.testing.TestStreams;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class PubSubTopologyTest {
    @RegisterExtension
    static final TopologyTestExtension EXTENSION = TopologyTestExtension.withBackend("pubsub");

    @TestStreams PrefabStreams streams;
    @TestStreams TopologySession topology;

    record Input(String key, int value) implements Keyed<String> {}
    record Output(String key, int value) implements Keyed<String> {}

    @Test
    void runsBackendNeutralTopologyWithPubSubFactory() {
        streams.from(Input.class).map(value -> new Output(value.key(), value.value() * 2)).to(Output.class);

        topology.input(Input.class).send(new Input("a", 3));

        assertThat(topology.output(Output.class).values()).containsExactly(new Output("a", 6));
    }
}
