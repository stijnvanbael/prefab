package be.appify.prefab.streams.topology;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.streams.JoinWindow;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.testing.TopologyTestHarness;
import be.appify.prefab.streams.topology.TopologyFixtures.CountOrderProcessor;
import be.appify.prefab.streams.topology.TopologyFixtures.IncomingOrder;
import be.appify.prefab.streams.topology.TopologyFixtures.JoinedOrder;
import be.appify.prefab.streams.topology.TopologyFixtures.OrderEvent;
import be.appify.prefab.streams.topology.TopologyFixtures.ProcessedOrder;
import be.appify.prefab.streams.topology.TopologyFixtures.ShippingUpdate;
import be.appify.prefab.streams.topology.TopologyFixtures.WordBatch;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class StepNamingTopologyTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("operators")
    void operator_shouldUseStableRepresentativeStepNames(
            String operator,
            Consumer<PrefabStreams> declaration,
            List<String> expectedSteps
    ) {
        var firstDescription = describe(declaration);
        var secondDescription = describe(declaration);

        assertThat(firstDescription)
                .isEqualTo(secondDescription)
                .contains(expectedSteps);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("repeatedOperators")
    void operator_shouldKeepStepNamesUniqueWithinOneTopology(
            String operator,
            Consumer<PrefabStreams> declaration,
            List<String> expectedSteps
    ) {
        assertThat(describe(declaration)).contains(expectedSteps);
    }

    static Stream<Arguments> operators() {
        return Stream.of(
                Arguments.of("branch",
                        (Consumer<PrefabStreams>) streams -> streams.from(IncomingOrder.class)
                                .branch(order -> order.customer().id().startsWith("A"))
                                .to("orders.a"),
                        List.of("branch-incoming-order", "branch-incoming-order-matched")),
                Arguments.of("filter",
                        (Consumer<PrefabStreams>) streams -> streams.from(IncomingOrder.class)
                                .filter(order -> order.customer().id().startsWith("A"))
                                .to("orders.filtered"),
                        List.of("filter-incoming-order")),
                Arguments.of("map",
                        (Consumer<PrefabStreams>) streams -> streams.from(IncomingOrder.class)
                                .map(order -> new ProcessedOrder(order.orderId(), order.customer()))
                                .to(ProcessedOrder.class),
                        List.of("map-incoming-order")),
                Arguments.of("flatMap",
                        (Consumer<PrefabStreams>) streams -> streams.from(WordBatch.class)
                                .flatMap(batch -> List.of(new WordBatch(batch.batchId(), batch.words())))
                                .to("words.out"),
                        List.of("flat-map-word-batch")),
                Arguments.of("branchSubtype",
                        (Consumer<PrefabStreams>) streams -> streams.from(IncomingOrder.class)
                                .branch(IncomingOrder.class)
                                .to("orders.out"),
                        List.of("branch-subtype-incoming-order", "branch-subtype-incoming-order-cast")),
                Arguments.of("merge",
                        (Consumer<PrefabStreams>) streams -> {
                            var source = streams.from(IncomingOrder.class);
                            source.filter(order -> order.customer().id().startsWith("A"))
                                    .merge(source.filter(order -> order.customer().id().startsWith("B")))
                                    .to(IncomingOrder.class);
                        },
                        List.of("merge-incoming-order")),
                Arguments.of("join",
                        (Consumer<PrefabStreams>) streams -> streams.from(IncomingOrder.class)
                                .join(
                                        streams.from(ShippingUpdate.class),
                                        JoinWindow.of(Duration.ofSeconds(10), Duration.ofSeconds(1)),
                                        (order, shipping) -> new JoinedOrder(
                                                order.orderId(), order.customer(), shipping.status())
                                )
                                .to(JoinedOrder.class),
                        List.of("join-incoming-order-shipping-update")),
                Arguments.of("process",
                        (Consumer<PrefabStreams>) streams -> streams.from(IncomingOrder.class)
                                .process(new CountOrderProcessor())
                                .to("prefab-streams-test-order-count"),
                        List.of("process-incoming-order"))
        );
    }

    static Stream<Arguments> repeatedOperators() {
        return Stream.of(
                Arguments.of("branch",
                        (Consumer<PrefabStreams>) streams -> {
                            var source = streams.from(IncomingOrder.class);
                            source.branch(order -> order.customer().id().startsWith("A")).to("orders.a");
                            source.branch(order -> order.customer().id().startsWith("B")).to("orders.b");
                        },
                        List.of("branch-incoming-order", "branch-incoming-order-matched", "branch-incoming-order-2")),
                Arguments.of("filter",
                        (Consumer<PrefabStreams>) streams -> {
                            var source = streams.from(IncomingOrder.class);
                            source.filter(order -> order.customer().id().startsWith("A")).to("orders.a");
                            source.filter(order -> order.customer().id().startsWith("B")).to("orders.b");
                        },
                        List.of("filter-incoming-order", "filter-incoming-order-2")),
                Arguments.of("map",
                        (Consumer<PrefabStreams>) streams -> {
                            var source = streams.from(IncomingOrder.class);
                            source.map(order -> new ProcessedOrder(order.orderId(), order.customer()))
                                    .to(ProcessedOrder.class);
                            source.map(order -> new ProcessedOrder(
                                            order.orderId(), Reference.fromId(order.customer().id() + "-done")))
                                    .to(ProcessedOrder.class);
                        },
                        List.of("map-incoming-order", "map-incoming-order-2")),
                Arguments.of("branchSubtype",
                        (Consumer<PrefabStreams>) streams -> {
                            var classified = streams.from(IncomingOrder.class)
                                    .map(order -> order.customer().id().length() <= 4
                                            ? (OrderEvent) new OrderEvent.OrderCreated(order.orderId(), order.customer())
                                            : new OrderEvent.OrderShipped(order.orderId()));
                            classified.branch(OrderEvent.OrderCreated.class).to("orders.created");
                            classified.branch(OrderEvent.OrderShipped.class).to("orders.shipped");
                        },
                        List.of("branch-subtype-order-created", "branch-subtype-order-shipped"))
        );
    }

    private static String describe(Consumer<PrefabStreams> declaration) {
        var harness = TopologyTestHarness.create("test");
        declaration.accept(harness.streams());
        try (var session = harness.start()) {
            return session.inspect().description();
        }
    }
}
