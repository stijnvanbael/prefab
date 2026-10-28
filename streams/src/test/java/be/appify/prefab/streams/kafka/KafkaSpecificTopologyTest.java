package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamBackend;
import be.appify.prefab.streams.StreamBreakoutAdapter;
import be.appify.prefab.streams.testing.TestStreams;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestExtension;
import be.appify.prefab.streams.topology.TopologyFixtures.CountOrderProcessor;
import be.appify.prefab.streams.topology.TopologyFixtures.Customer;
import be.appify.prefab.streams.topology.TopologyFixtures.CustomerProfile;
import be.appify.prefab.streams.topology.TopologyFixtures.EnrichOrderWithProfileProcessor;
import be.appify.prefab.streams.topology.TopologyFixtures.IncomingOrder;
import be.appify.prefab.streams.topology.TopologyFixtures.Order;
import be.appify.prefab.streams.topology.TopologyFixtures.OrderCount;
import be.appify.prefab.streams.topology.TopologyFixtures.ProcessedOrder;
import java.util.Collection;
import java.util.List;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests for behaviour that only exists on the Kafka Streams backend. */
@ExtendWith(TopologyTestExtension.class)
class KafkaSpecificTopologyTest {
    @TestStreams
    PrefabStreams streams;
    @TestStreams
    TopologySession topology;

    @Test
    void breakout_shouldApplyKafkaNativeFragmentWithinPrefabPipeline() {
        streams.from(IncomingOrder.class)
                .breakout(new KafkaStreamBreakoutAdapter<Reference<Order>, Reference<Order>, IncomingOrder, IncomingOrder>(
                        nativeStream -> nativeStream.selectKey(
                                (key, value) -> Reference.fromId("native-" + value.orderId().id()),
                                Named.as("native-key")
                        )
                ))
                .map(order -> new ProcessedOrder(order.orderId(), Reference.fromId(order.customer().id().toUpperCase())))
                .to(ProcessedOrder.class);

        topology.input(IncomingOrder.class)
                .send(new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice")))
                .send(new IncomingOrder(Reference.fromId("o-2"), Reference.fromId("bob")));

        assertThat(topology.output(ProcessedOrder.class).values())
                .containsExactlyInAnyOrder(
                        new ProcessedOrder(Reference.fromId("o-1"), Reference.fromId("ALICE")),
                        new ProcessedOrder(Reference.fromId("o-2"), Reference.fromId("BOB"))
                );
    }

    @Test
    void breakout_shouldFailFastWhenAdapterTargetsDifferentBackend() {
        assertThatThrownBy(() -> streams.from(IncomingOrder.class).breakout(new NonKafkaBreakoutAdapter()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported breakout backend");
    }

    @Test
    void breakout_shouldFailFastWhenAdapterReturnsNonKStream() {
        assertThatThrownBy(() -> streams.from(IncomingOrder.class).breakout(new InvalidKafkaReturnTypeAdapter()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must return a KStream");
    }

    @Test
    void process_shouldFailFastForUndeclaredGlobalStoreBinding() {
        var undeclared = new KafkaPrefabGlobalStoreAdapter<Reference<Customer>, CustomerProfile>(
                "global-undeclared", new StreamsBuilder()::build);

        assertThatThrownBy(() -> streams.from(IncomingOrder.class)
                .process(new EnrichOrderWithProfileProcessor(undeclared)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Global store 'global-undeclared' is not declared");
    }

    @Test
    void process_shouldFailFastForUndeclaredStoreBinding() {
        assertThatThrownBy(() -> streams.from(IncomingOrder.class)
                .process(new CountOrderProcessor() {
                    @Override
                    public Collection<be.appify.prefab.streams.Store<?, ?>> stateStores() {
                        return List.of(new KafkaPrefabStoreAdapter<>("undeclared-store"));
                    }
                }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Store 'undeclared-store' is not declared");
    }

    private static final class NonKafkaBreakoutAdapter
            implements StreamBreakoutAdapter<Reference<Order>, IncomingOrder, Reference<Order>, IncomingOrder,
            KStream<Reference<Order>, IncomingOrder>, KStream<Reference<Order>, IncomingOrder>> {

        @Override
        public StreamBackend backend() {
            return StreamBackend.CUSTOM;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<KStream<Reference<Order>, IncomingOrder>> nativeInputType() {
            return (Class<KStream<Reference<Order>, IncomingOrder>>) (Class<?>) KStream.class;
        }

        @Override
        public KStream<Reference<Order>, IncomingOrder> apply(KStream<Reference<Order>, IncomingOrder> nativeStream) {
            return nativeStream;
        }
    }

    private static final class InvalidKafkaReturnTypeAdapter
            implements StreamBreakoutAdapter<Reference<Order>, IncomingOrder, Reference<Order>, IncomingOrder,
            KStream<Reference<Order>, IncomingOrder>, Object> {

        @Override
        public StreamBackend backend() {
            return StreamBackend.KAFKA;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<KStream<Reference<Order>, IncomingOrder>> nativeInputType() {
            return (Class<KStream<Reference<Order>, IncomingOrder>>) (Class<?>) KStream.class;
        }

        @Override
        public Object apply(KStream<Reference<Order>, IncomingOrder> nativeStream) {
            return "not-a-stream";
        }
    }
}
