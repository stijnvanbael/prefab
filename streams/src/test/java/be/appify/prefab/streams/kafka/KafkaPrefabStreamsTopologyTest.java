package be.appify.prefab.streams.kafka;

import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.streams.ContextualStreamProcessor;
import be.appify.prefab.streams.JoinWindow;
import be.appify.prefab.streams.StatefulStreamProcessor;
import be.appify.prefab.streams.Store;
import be.appify.prefab.streams.StreamBackend;
import be.appify.prefab.streams.StreamBreakoutAdapter;
import be.appify.prefab.streams.StreamRecord;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tools.jackson.databind.json.JsonMapper;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Named;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaPrefabStreamsTopologyTest {
    @Test
    void filter_shouldDropRecordsNotMatchingPredicate() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        var topology = test.streams().from(IncomingOrder.class)
                .filter(order -> order.customer().id().startsWith("A"))
                .to("orders.filtered");

        try (var topologyTest = test.run(topology)) {
            var inputTopic = topologyTest.input(IncomingOrder.class);
            var outputTopic = topologyTest.rawOutput("orders.filtered");

            inputTopic.pipeInput(Reference.fromId("o-1"), new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("Alice")));
            inputTopic.pipeInput(Reference.fromId("o-2"), new IncomingOrder(Reference.fromId("o-2"), Reference.fromId("Bob")));
            inputTopic.pipeInput(Reference.fromId("o-3"), new IncomingOrder(Reference.fromId("o-3"), Reference.fromId("Anna")));

            assertThat(outputTopic.readValuesToList()).hasSize(2);
        }
    }

    @Test
    void map_shouldTransformValues() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        var topology = test.streams().from(IncomingOrder.class)
                .map(order -> new ProcessedOrder(order.orderId(), Reference.fromId(order.customer().id().toUpperCase())))
                .to(ProcessedOrder.class);

        try (var topologyTest = test.run(topology)) {
            var inputTopic = topologyTest.input(IncomingOrder.class);
            var outputTopic = topologyTest.output(ProcessedOrder.class);

            inputTopic.pipeInput(Reference.fromId("o-1"), new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("Alice")));

            var forwarded = outputTopic.readValue();
            assertThat(forwarded).isInstanceOf(ProcessedOrder.class);
            assertThat(forwarded).isEqualTo(new ProcessedOrder(Reference.fromId("o-1"), Reference.fromId("ALICE")));
        }
    }

    @Test
    void flatMap_shouldExpandOneRecordToMany() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        var topology = test.streams().from(WordBatch.class)
                .flatMap(batch -> Stream.of(batch.words().split(","))
                        .map(word -> new WordBatch(batch.batchId(), word))
                        .toList())
                .to("words.out");

        try (var topologyTest = test.run(topology)) {
            var inputTopic = topologyTest.input(WordBatch.class);
            var outputTopic = topologyTest.rawOutput("words.out");

            inputTopic.pipeInput(Reference.fromId("b-1"), new WordBatch(Reference.fromId("b-1"), "hello,world,foo"));

            assertThat(outputTopic.readValuesToList()).hasSize(3);
        }
    }

    @Test
    void branch_shouldUseStableRepresentativeStepNames() {
        var firstDescription = branchTopologyDescription();
        var secondDescription = branchTopologyDescription();

        assertThat(firstDescription)
                .isEqualTo(secondDescription)
                .contains("branch-incoming-order")
                .contains("branch-incoming-order-matched");
    }

    @Test
    void branch_shouldKeepStepNamesUniqueWithinOneTopology() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        var streams = test.streams();
        var source = streams.from(IncomingOrder.class);

        source.branch(order -> order.customer().id().startsWith("A"))
                .to("orders.a");
        var topology = source.branch(order -> order.customer().id().startsWith("B"))
                .to("orders.b");

        var topologyDescription = topology.nativeTopology().describe().toString();

        assertThat(topologyDescription)
                .contains("branch-incoming-order")
                .contains("branch-incoming-order-matched")
                .contains("branch-incoming-order-2");
    }

    @Test
    void filter_shouldUseStableRepresentativeStepNames() {
        var firstDescription = filterTopologyDescription();
        var secondDescription = filterTopologyDescription();

        assertThat(firstDescription)
                .isEqualTo(secondDescription)
                .contains("filter-incoming-order");
    }

    @Test
    void filter_shouldKeepStepNamesUniqueWithinOneTopology() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        var source = test.streams().from(IncomingOrder.class);

        source.filter(order -> order.customer().id().startsWith("A")).to("orders.a");
        var topology = source.filter(order -> order.customer().id().startsWith("B")).to("orders.b");

        assertThat(topology.nativeTopology().describe().toString())
                .contains("filter-incoming-order")
                .contains("filter-incoming-order-2");
    }

    @Test
    void map_shouldUseStableRepresentativeStepNames() {
        var firstDescription = mapTopologyDescription();
        var secondDescription = mapTopologyDescription();

        assertThat(firstDescription)
                .isEqualTo(secondDescription)
                .contains("map-incoming-order");
    }

    @Test
    void map_shouldKeepStepNamesUniqueWithinOneTopology() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        var source = test.streams().from(IncomingOrder.class);

        source.map(order -> new ProcessedOrder(order.orderId(), Reference.fromId(order.customer().id().toUpperCase())))
                .to(ProcessedOrder.class);
        var topology = source.map(order -> new ProcessedOrder(order.orderId(), Reference.fromId(order.customer().id() + "-done")))
                .to(ProcessedOrder.class);

        assertThat(topology.nativeTopology().describe().toString())
                .contains("map-incoming-order")
                .contains("map-incoming-order-2");
    }

    @Test
    void flatMap_shouldUseStableRepresentativeStepNames() {
        var firstDescription = flatMapTopologyDescription();
        var secondDescription = flatMapTopologyDescription();

        assertThat(firstDescription)
                .isEqualTo(secondDescription)
                .contains("flat-map-word-batch");
    }

    @Test
    void branchSubtype_shouldUseStableRepresentativeStepNames() {
        var firstDescription = branchSubtypeTopologyDescription();
        var secondDescription = branchSubtypeTopologyDescription();

        assertThat(firstDescription)
                .isEqualTo(secondDescription)
                .contains("branch-subtype-incoming-order")
                .contains("branch-subtype-incoming-order-cast");
    }

    @Test
    void branchSubtype_shouldKeepStepNamesUniqueWithinOneTopology() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        var streams = test.streams();
        var classified = streams.from(IncomingOrder.class)
                .map(order -> order.customer().id().length() <= 4
                        ? (OrderEvent) new OrderEvent.OrderCreated(order.orderId(), order.customer())
                        : new OrderEvent.OrderShipped(order.orderId()));

        classified.branch(OrderEvent.OrderCreated.class).to("orders.created");
        var topology = classified.branch(OrderEvent.OrderShipped.class).to("orders.shipped");

        assertThat(topology.nativeTopology().describe().toString())
                .contains("branch-subtype-order-created")
                .contains("branch-subtype-order-shipped");
    }

    @Test
    void merge_shouldUseStableRepresentativeStepNames() {
        var firstDescription = mergeTopologyDescription();
        var secondDescription = mergeTopologyDescription();

        assertThat(firstDescription)
                .isEqualTo(secondDescription)
                .contains("merge-incoming-order");
    }

    @Test
    void join_shouldUseStableRepresentativeStepNames() {
        var firstDescription = joinTopologyDescription();
        var secondDescription = joinTopologyDescription();

        assertThat(firstDescription)
                .isEqualTo(secondDescription)
                .contains("join-incoming-order-shipping-update");
    }

    @Test
    void process_shouldUseStableRepresentativeStepNames() {
        var firstDescription = processTopologyDescription();
        var secondDescription = processTopologyDescription();

        assertThat(firstDescription)
                .isEqualTo(secondDescription)
                .contains("process-incoming-order");
    }


    @Test
    void join_shouldEmitResultWhenKeysMatchWithinWindow() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        var streams = test.streams();
        var topology = streams.from(IncomingOrder.class)
                .join(
                        streams.from(ShippingUpdate.class),
                        JoinWindow.of(Duration.ofSeconds(10), Duration.ofSeconds(1)),
                        (order, shipping) -> new JoinedOrder(order.orderId(), order.customer(), shipping.status())
                )
                .to(JoinedOrder.class);

        try (var topologyTest = test.run(topology)) {
            var ordersInputTopic = topologyTest.input(IncomingOrder.class);
            var shipmentsInputTopic = topologyTest.input(ShippingUpdate.class);
            var outputTopic = topologyTest.output(JoinedOrder.class);

            ordersInputTopic.pipeInput(
                    Reference.fromId("o-1"),
                    new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("Alice")),
                    Instant.ofEpochMilli(1_000L)
            );
            shipmentsInputTopic.pipeInput(
                    Reference.fromId("o-1"),
                    new ShippingUpdate(Reference.fromId("o-1"), "SHIPPED"),
                    Instant.ofEpochMilli(5_000L)
            );

            assertThat(outputTopic.readValue())
                    .isEqualTo(new JoinedOrder(Reference.fromId("o-1"), Reference.fromId("Alice"), "SHIPPED"));
        }
    }

    @Test
    void join_shouldNotEmitResultWhenKeysDoNotMatch() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        var streams = test.streams();
        var topology = streams.from(IncomingOrder.class)
                .join(
                        streams.from(ShippingUpdate.class),
                        JoinWindow.of(Duration.ofSeconds(10), Duration.ofSeconds(1)),
                        (order, shipping) -> new JoinedOrder(order.orderId(), order.customer(), shipping.status())
                )
                .to(JoinedOrder.class);

        try (var topologyTest = test.run(topology)) {
            var ordersInputTopic = topologyTest.input(IncomingOrder.class);
            var shipmentsInputTopic = topologyTest.input(ShippingUpdate.class);
            var outputTopic = topologyTest.output(JoinedOrder.class);

            ordersInputTopic.pipeInput(
                    Reference.fromId("o-1"),
                    new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("Alice")),
                    Instant.ofEpochMilli(1_000L)
            );
            shipmentsInputTopic.pipeInput(
                    Reference.fromId("o-2"),
                    new ShippingUpdate(Reference.fromId("o-2"), "SHIPPED"),
                    Instant.ofEpochMilli(5_000L)
            );

            assertThat(outputTopic.readValuesToList()).isEmpty();
        }
    }

    @Test
    void join_shouldNotEmitResultWhenRecordsAreOutsideWindow() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        var streams = test.streams();
        var topology = streams.from(IncomingOrder.class)
                .join(
                        streams.from(ShippingUpdate.class),
                        JoinWindow.of(Duration.ofSeconds(2), Duration.ZERO),
                        (order, shipping) -> new JoinedOrder(order.orderId(), order.customer(), shipping.status())
                )
                .to(JoinedOrder.class);

        try (var topologyTest = test.run(topology)) {
            var ordersInputTopic = topologyTest.input(IncomingOrder.class);
            var shipmentsInputTopic = topologyTest.input(ShippingUpdate.class);
            var outputTopic = topologyTest.output(JoinedOrder.class);

            ordersInputTopic.pipeInput(
                    Reference.fromId("o-1"),
                    new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("Alice")),
                    Instant.ofEpochMilli(1_000L)
            );
            shipmentsInputTopic.pipeInput(
                    Reference.fromId("o-1"),
                    new ShippingUpdate(Reference.fromId("o-1"), "SHIPPED"),
                    Instant.ofEpochMilli(5_000L)
            );

            assertThat(outputTopic.readValuesToList()).isEmpty();
        }
    }

    @Test
    void breakout_shouldApplyKafkaNativeFragmentWithinPrefabPipeline() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        var topology = test.streams().from(IncomingOrder.class)
                .breakout(new KafkaStreamBreakoutAdapter<Reference<Order>, Reference<Order>, IncomingOrder, IncomingOrder>(
                        nativeStream -> nativeStream.selectKey(
                                (key, value) -> Reference.fromId("native-" + value.orderId().id()),
                                Named.as("native-key")
                        )
                ))
                .map(order -> new ProcessedOrder(order.orderId(), Reference.fromId(order.customer().id().toUpperCase())))
                .to(ProcessedOrder.class);

        try (var topologyTest = test.run(topology)) {
            var inputTopic = topologyTest.input(IncomingOrder.class);
            var outputTopic = topologyTest.output(ProcessedOrder.class);

            inputTopic.pipeInput(Reference.fromId("o-1"), new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice")));
            inputTopic.pipeInput(Reference.fromId("o-2"), new IncomingOrder(Reference.fromId("o-2"), Reference.fromId("bob")));

            var output = outputTopic.readValuesToList();
            assertThat(output)
                    .containsExactlyInAnyOrder(
                            new ProcessedOrder(Reference.fromId("o-1"), Reference.fromId("ALICE")),
                            new ProcessedOrder(Reference.fromId("o-2"), Reference.fromId("BOB"))
                    );
        }
    }

    @Test
    void breakout_shouldFailFastWhenAdapterTargetsDifferentBackend() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        assertThatThrownBy(() -> test.streams().from(IncomingOrder.class)
                .breakout(new NonKafkaBreakoutAdapter()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported breakout backend");
    }

    @Test
    void breakout_shouldFailFastWhenAdapterReturnsNonKStream() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        assertThatThrownBy(() -> test.streams().from(IncomingOrder.class)
                .breakout(new InvalidKafkaReturnTypeAdapter()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must return a KStream");
    }

    @Test
    void process_shouldPerformStatefulOperations() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        var countDefinition = test.streams().from(IncomingOrder.class)
                .process(new CountOrderProcessor())
                .to("prefab-streams-test-order-count");

        try (var topologyTest = test.run(countDefinition)) {
            var input = topologyTest.input(IncomingOrder.class);
            var output = topologyTest.output(OrderCount.class);

            input.pipeInput(Reference.fromId("o-1"), new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice")));
            input.pipeInput(Reference.fromId("o-2"), new IncomingOrder(Reference.fromId("o-2"), Reference.fromId("bob")));
            input.pipeInput(Reference.fromId("o-3"), new IncomingOrder(Reference.fromId("o-3"), Reference.fromId("alice")));

            assertThat(output.readValuesToList())
                    .containsExactlyInAnyOrder(
                            new OrderCount(Reference.fromId("alice"), 1),
                            new OrderCount(Reference.fromId("bob"), 1),
                            new OrderCount(Reference.fromId("alice"), 2)
                    );
        }
    }

    @Test
    void sharedStore_shouldBeReusableAcrossProcessorsAndBranches() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        var streams = test.streams();
        var customerProfiles = streams.sharedStore("customer-profiles", CustomerProfile.class);

        streams.from(CustomerProfileUpdated.class)
                .process(new UpdateCustomerProfileProcessor(customerProfiles), customerProfiles)
                .to("customer-profile-updates.audit");

        var topology = streams.from(IncomingOrder.class)
                .process(new EnrichOrderWithProfileProcessor(customerProfiles), customerProfiles)
                .to(OrderProfileView.class);

        try (var topologyTest = test.run(topology)) {
            var profileUpdates = topologyTest.input(CustomerProfileUpdated.class);
            var orders = topologyTest.input(IncomingOrder.class);
            var output = topologyTest.output(OrderProfileView.class);

            profileUpdates.pipeInput(
                    Reference.fromId("alice"),
                    new CustomerProfileUpdated(Reference.fromId("alice"), "SILVER")
            );
            orders.pipeInput(
                    Reference.fromId("o-1"),
                    new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice"))
            );
            profileUpdates.pipeInput(
                    Reference.fromId("alice"),
                    new CustomerProfileUpdated(Reference.fromId("alice"), "GOLD")
            );
            orders.pipeInput(
                    Reference.fromId("o-2"),
                    new IncomingOrder(Reference.fromId("o-2"), Reference.fromId("alice"))
            );

            assertThat(output.readValuesToList())
                    .containsExactly(
                            new OrderProfileView(Reference.fromId("o-1"), Reference.fromId("alice"), "SILVER"),
                            new OrderProfileView(Reference.fromId("o-2"), Reference.fromId("alice"), "GOLD")
                    );
        }
    }

    @Test
    void sharedStore_shouldReturnSameDeclarationForCompatibleReuse() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        var streams = test.streams();

        var first = streams.sharedStore("customer-profiles", CustomerProfile.class);
        var second = streams.sharedStore("customer-profiles", CustomerProfile.class);

        assertThat(first).isSameAs(second);
    }

    @Test
    void sharedStore_shouldFailFastForIncompatibleDeclarations() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        var streams = test.streams();
        streams.sharedStore("shared-store", CustomerProfile.class);

        assertThatThrownBy(() -> streams.sharedStore("shared-store", OrderCount.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already declared")
                .hasMessageContaining("shared-store");
    }

    @Test
    void process_shouldFailFastForConflictingStoreBindings() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        assertThatThrownBy(() -> test.streams().from(IncomingOrder.class)
                .process(
                        new CountOrderProcessor(),
                        new KafkaPrefabStoreAdapter<>("duplicate-store"),
                        new KafkaPrefabStoreAdapter<>("duplicate-store")
                ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate store binding 'duplicate-store'");
    }

    @Test
    void to_shouldEmitPlainUtf8KeyBytesForSingleFieldRecordKeys() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        var topology = test.streams().from(IncomingOrder.class)
                .to("orders.single-field-key");

        try (var topologyTest = test.run(topology)) {
            var input = topologyTest.input(IncomingOrder.class);
            var output = topologyTest.rawOutputBytes("orders.single-field-key");

            input.pipeInput(Reference.fromId("o-1"), new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice")));

            var record = output.readRecord();
            assertThat(new String(record.key(), UTF_8)).isEqualTo("o-1");
        }
    }

    @Test
    void to_shouldEmitJsonKeyBytesForCompositeRecordKeys() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        var topology = test.streams().from(CompositeIncomingOrder.class)
                .to("orders.composite-key");

        try (var topologyTest = test.run(topology)) {
            var key = new CompositeOrderKey("be", "o-1");
            var input = topologyTest.input(CompositeIncomingOrder.class, CompositeOrderKey.class);
            var output = topologyTest.rawOutputBytes("orders.composite-key");

            input.pipeInput(key, new CompositeIncomingOrder(key, "alice"));

            var record = output.readRecord();
            assertThat(new String(record.key(), UTF_8))
                    .contains("\"biddingZone\":\"be\"")
                    .contains("\"orderId\":\"o-1\"");
        }
    }

    @Test
    void join_shouldEmitResultWhenCompositeKeysMatchWithinWindow() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        var streams = test.streams();
        var topology = streams.from(CompositeIncomingOrder.class)
                .join(
                        streams.from(CompositeShippingUpdate.class),
                        JoinWindow.of(Duration.ofSeconds(10), Duration.ofSeconds(1)),
                        (order, shipping) -> new CompositeJoinedOrder(order.key(), order.customer(), shipping.status())
                )
                .to(CompositeJoinedOrder.class);

        try (var topologyTest = test.run(topology)) {
            var key = new CompositeOrderKey("be", "o-1");
            var orders = topologyTest.input(CompositeIncomingOrder.class, CompositeOrderKey.class);
            var updates = topologyTest.input(CompositeShippingUpdate.class, CompositeOrderKey.class);
            var output = topologyTest.output(CompositeJoinedOrder.class, CompositeOrderKey.class);

            orders.pipeInput(key, new CompositeIncomingOrder(key, "alice"), Instant.ofEpochMilli(1_000L));
            updates.pipeInput(key, new CompositeShippingUpdate(key, "SHIPPED"), Instant.ofEpochMilli(4_000L));

            assertThat(output.readValue())
                    .isEqualTo(new CompositeJoinedOrder(key, "alice", "SHIPPED"));
        }
    }

    @Test
    void process_shouldPerformStatefulOperationsForCompositeKeys() {
        var test = KafkaTopologyTestBootstrap.bootstrap();

        var topology = test.streams().from(CompositeIncomingOrder.class)
                .process(new CountCompositeOrderProcessor())
                .to("prefab-streams-test-composite-count");

        try (var topologyTest = test.run(topology)) {
            var beO1 = new CompositeOrderKey("be", "o-1");
            var nlO1 = new CompositeOrderKey("nl", "o-1");
            var input = topologyTest.input(CompositeIncomingOrder.class, CompositeOrderKey.class);
            var output = topologyTest.rawOutputBytes("prefab-streams-test-composite-count");

            input.pipeInput(beO1, new CompositeIncomingOrder(beO1, "alice"));
            input.pipeInput(nlO1, new CompositeIncomingOrder(nlO1, "bob"));
            input.pipeInput(beO1, new CompositeIncomingOrder(beO1, "alice"));

            var mapper = JsonMapper.builder().findAndAddModules().build();
            var values = output.readValuesToList().stream()
                    .map(bytes -> {
                        try {
                            return mapper.readValue(bytes, CompositeOrderCount.class);
                        } catch (Exception e) {
                            throw new IllegalStateException("Failed to deserialize composite order count", e);
                        }
                    })
                    .toList();

            assertThat(values)
                    .containsExactlyInAnyOrder(
                            new CompositeOrderCount(beO1, 1),
                            new CompositeOrderCount(nlO1, 1),
                            new CompositeOrderCount(beO1, 2)
                    );
        }
    }

    @Test
    void aggregate() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        var streams = test.streams();

        var countDefinition = streams.from(IncomingOrder.class)
                .aggregate(
                        IncomingOrder::customer,
                        ordersForCustomer -> new OrderCount(ordersForCustomer.getFirst().customer(), ordersForCustomer.size()),
                        count -> true
                )
                .to(OrderCount.class);

        try (var topologyTest = test.run(countDefinition)) {
            var input = topologyTest.input(IncomingOrder.class);
            var output = topologyTest.output(OrderCount.class);

            input.pipeInput(Reference.fromId("o-1"), new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice")));
            input.pipeInput(Reference.fromId("o-2"), new IncomingOrder(Reference.fromId("o-2"), Reference.fromId("bob")));
            input.pipeInput(Reference.fromId("o-3"), new IncomingOrder(Reference.fromId("o-3"), Reference.fromId("alice")));

            assertThat(output.readValuesToList())
                    .containsExactlyInAnyOrder(
                            new OrderCount(Reference.fromId("alice"), 1),
                            new OrderCount(Reference.fromId("bob"), 1),
                            new OrderCount(Reference.fromId("alice"), 2)
                    );
        }
    }

    @Test
    void aggregate_storeNameReflectsConcreteKeyAndValueTypes() {
        // The stream was created from IncomingOrder.class so V = IncomingOrder is known at
        // topology-build time via knownValueType().  Java's standard LambdaMetafactory erases
        // the groupBy return type to Object at runtime, so the key type (KO = Reference) cannot
        // be extracted without serialisable lambdas or an explicit parameter.
        // The store name therefore encodes only the value class: aggregation-incoming-order.
        var test = KafkaTopologyTestBootstrap.bootstrap();
        var definition = test.streams().from(IncomingOrder.class)
                .aggregate(
                        IncomingOrder::customer,
                        orders -> new OrderCount(orders.getFirst().customer(), orders.size()),
                        count -> true
                )
                .to(OrderCount.class);

        var topologyDescription = definition.nativeTopology().describe().toString();
        assertThat(topologyDescription).contains("aggregation-incoming-order");
    }

    @Test
    void multipleStreams_shouldBeJoinedInOneTopology() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        var streams = test.streams();

        streams.from(IncomingOrder.class)
                .map(order -> new ProcessedOrder(order.orderId(), Reference.fromId(order.customer().id().toUpperCase())))
                .to(ProcessedOrder.class);

        var topology = streams.from(ProcessedOrder.class)
                .map(order -> new ShippingUpdate(order.orderId(), "PENDING"))
                .to(ShippingUpdate.class);

        try (var topologyTest = test.run(topology)) {
            var input = topologyTest.input(IncomingOrder.class);
            var output = topologyTest.output(ShippingUpdate.class);

            input.pipeInput(Reference.fromId("o-1"), new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice")));

            assertThat(output.readValuesToList())
                    .containsExactlyInAnyOrder(
                            new ShippingUpdate(Reference.fromId("o-1"), "PENDING")
                    );
        }
    }

    @Test
    void joinAfterMap_shouldUseTheRightSerdes() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        var streams = test.streams();

        var topology = streams.from(IncomingOrder.class)
                .map(order -> new ProcessedOrder(order.orderId(), Reference.fromId(order.customer().id().toUpperCase())))
                .join(
                        streams.from(ShippingUpdate.class),
                        JoinWindow.of(Duration.ofSeconds(2), Duration.ZERO),
                        (order, shipping) -> new JoinedOrder(order.orderId(), order.customer(), shipping.status())
                )
                .to(JoinedOrder.class);

        try (var topologyTest = test.run(topology)) {
            var inputOrder = topologyTest.input(IncomingOrder.class);
            var inputShippingUpdate = topologyTest.input(ShippingUpdate.class);
            var output = topologyTest.output(JoinedOrder.class);

            inputOrder.pipeInput(Reference.fromId("o-1"), new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice")));
            inputShippingUpdate.pipeInput(Reference.fromId("o-1"), new ShippingUpdate(Reference.fromId("o-1"), "SHIPPED"));

            assertThat(output.readValuesToList())
                    .containsExactlyInAnyOrder(
                            new JoinedOrder(Reference.fromId("o-1"), Reference.fromId("ALICE"), "SHIPPED")
                    );
        }
    }

    @Test
    void mergeAfterMap_shouldUseTheRightSerdes() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        var streams = test.streams();

        var orders = streams.from(IncomingOrder.class)
                .map(order -> new OrderEvent.OrderCreated(order.orderId(), order.customer()));
        var shippingUpdates = streams.from(ShippingUpdate.class)
                .filter(update -> "SHIPPED".equals(update.status()))
                .map(update -> new OrderEvent.OrderShipped(update.orderId()));
        var topology = streams.merge(orders, shippingUpdates)
                .to(OrderEvent.class);

        try (var topologyTest = test.run(topology)) {
            var inputOrder = topologyTest.input(IncomingOrder.class);
            var inputShippingUpdate = topologyTest.input(ShippingUpdate.class);
            var output = topologyTest.output(OrderEvent.class);

            inputOrder.pipeInput(Reference.fromId("o-1"), new IncomingOrder(Reference.fromId("o-1"), Reference.fromId("alice")));
            inputShippingUpdate.pipeInput(Reference.fromId("o-1"), new ShippingUpdate(Reference.fromId("o-1"), "SHIPPED"));

            assertThat(output.readValuesToList())
                    .containsExactlyInAnyOrder(
                            new OrderEvent.OrderCreated(Reference.fromId("o-1"), Reference.fromId("alice")),
                            new OrderEvent.OrderShipped(Reference.fromId("o-1"))
                    );
        }
    }

    record IncomingOrder(Reference<Order> orderId, Reference<Customer> customer) implements Keyed<Reference<Order>> {
        @Override
        public Reference<Order> key() {
            return orderId;
        }
    }

    record ProcessedOrder(Reference<Order> orderId, Reference<Customer> customer) implements Keyed<Reference<Order>> {
        @Override
        public Reference<Order> key() {
            return orderId;
        }
    }

    record Order() {
    }

    record Customer() {
    }

    record WordBatch(Reference<Batch> batchId, String words) implements Keyed<Reference<Batch>> {
        @Override
        public Reference<Batch> key() {
            return batchId;
        }
    }

    record Batch() {
    }

    record ShippingUpdate(Reference<Order> orderId, String status) implements Keyed<Reference<Order>> {
        @Override
        public Reference<Order> key() {
            return orderId;
        }
    }

    record JoinedOrder(Reference<Order> orderId, Reference<Customer> customer,
                       String shippingStatus) implements Keyed<Reference<Order>> {
        @Override
        public Reference<Order> key() {
            return orderId;
        }
    }

    record OrderCount(Reference<Customer> customer, int numberOfOrders) implements Keyed<Reference<Customer>> {
        @Override
        public Reference<Customer> key() {
            return customer;
        }
    }

    record CustomerProfile(Reference<Customer> customerId, String tier) implements Keyed<Reference<Customer>> {
        @Override
        public Reference<Customer> key() {
            return customerId;
        }
    }

    record CustomerProfileUpdated(Reference<Customer> customerId, String tier) implements Keyed<Reference<Customer>> {
        @Override
        public Reference<Customer> key() {
            return customerId;
        }
    }

    record OrderProfileView(Reference<Order> orderId, Reference<Customer> customer, String tier)
            implements Keyed<Reference<Order>> {
        @Override
        public Reference<Order> key() {
            return orderId;
        }
    }

    record CompositeOrderKey(String biddingZone, String orderId) {
    }

    record CompositeIncomingOrder(CompositeOrderKey key, String customer) implements Keyed<CompositeOrderKey> {
    }

    record CompositeShippingUpdate(CompositeOrderKey key, String status) implements Keyed<CompositeOrderKey> {
    }

    record CompositeJoinedOrder(CompositeOrderKey key, String customer, String shippingStatus)
            implements Keyed<CompositeOrderKey> {
    }

    record CompositeOrderCount(CompositeOrderKey key, int count) implements Keyed<CompositeOrderKey> {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    sealed interface OrderEvent extends Keyed<Reference<Order>> permits OrderEvent.OrderCreated, OrderEvent.OrderShipped {
        @Override
        default Reference<Order> key() {
            return orderId();
        }

        Reference<Order> orderId();

        record OrderCreated(Reference<Order> orderId, Reference<Customer> customer) implements OrderEvent {}

        record OrderShipped(Reference<Order> orderId) implements OrderEvent {}
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

    private static class CountOrderProcessor
            extends StatefulStreamProcessor<Reference<Order>, IncomingOrder, Reference<Customer>, OrderCount> {
        public CountOrderProcessor() {
            super(OrderCount.class);
        }

        @Override
        public void process(StreamRecord<Reference<Order>, IncomingOrder> streamRecord) {
            var orderCount = store(OrderCount.class).putOrUpdate(
                    streamRecord.value().customer(),
                    () -> new OrderCount(streamRecord.value().customer(), 1),
                    existing -> new OrderCount(existing.customer(), existing.numberOfOrders() + 1));
            forward(orderCount.key(), orderCount);
        }
    }

    private static final class UpdateCustomerProfileProcessor
            extends ContextualStreamProcessor<Reference<Customer>, CustomerProfileUpdated, Reference<Customer>, CustomerProfileUpdated> {
        private final Store<Reference<Customer>, CustomerProfile> customerProfiles;

        private UpdateCustomerProfileProcessor(Store<Reference<Customer>, CustomerProfile> customerProfiles) {
            this.customerProfiles = customerProfiles;
        }

        @Override
        public void process(StreamRecord<Reference<Customer>, CustomerProfileUpdated> streamRecord) {
            customerProfiles.put(
                    streamRecord.key(),
                    new CustomerProfile(streamRecord.key(), streamRecord.value().tier())
            );
            forward(streamRecord.key(), streamRecord.value());
        }
    }

    private static final class EnrichOrderWithProfileProcessor
            extends ContextualStreamProcessor<Reference<Order>, IncomingOrder, Reference<Order>, OrderProfileView> {
        private final Store<Reference<Customer>, CustomerProfile> customerProfiles;

        private EnrichOrderWithProfileProcessor(Store<Reference<Customer>, CustomerProfile> customerProfiles) {
            this.customerProfiles = customerProfiles;
        }

        @Override
        public void process(StreamRecord<Reference<Order>, IncomingOrder> streamRecord) {
            var tier = customerProfiles.get(streamRecord.value().customer())
                    .map(CustomerProfile::tier)
                    .orElse("UNKNOWN");
            forward(
                    streamRecord.key(),
                    new OrderProfileView(streamRecord.key(), streamRecord.value().customer(), tier)
            );
        }
    }

    private static class CountCompositeOrderProcessor
            extends StatefulStreamProcessor<CompositeOrderKey, CompositeIncomingOrder, CompositeOrderKey, CompositeOrderCount> {

        CountCompositeOrderProcessor() {
            super(CompositeOrderCount.class);
        }

        @Override
        public void process(StreamRecord<CompositeOrderKey, CompositeIncomingOrder> streamRecord) {
            var count = store(CompositeOrderCount.class).putOrUpdate(
                    streamRecord.key(),
                    () -> new CompositeOrderCount(streamRecord.key(), 1),
                    existing -> new CompositeOrderCount(existing.key(), existing.count() + 1));
            forward(count.key(), count);
        }
    }

    private String branchTopologyDescription() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        var topology = test.streams().from(IncomingOrder.class)
                .branch(order -> order.customer().id().startsWith("A"))
                .to("orders.a");
        return topology.nativeTopology().describe().toString();
    }

    private String filterTopologyDescription() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        var topology = test.streams().from(IncomingOrder.class)
                .filter(order -> order.customer().id().startsWith("A"))
                .to("orders.filtered");
        return topology.nativeTopology().describe().toString();
    }

    private String mapTopologyDescription() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        var topology = test.streams().from(IncomingOrder.class)
                .map(order -> new ProcessedOrder(order.orderId(), Reference.fromId(order.customer().id().toUpperCase())))
                .to(ProcessedOrder.class);
        return topology.nativeTopology().describe().toString();
    }

    private String flatMapTopologyDescription() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        var topology = test.streams().from(WordBatch.class)
                .flatMap(batch -> Stream.of(batch.words().split(","))
                        .map(word -> new WordBatch(batch.batchId(), word))
                        .toList())
                .to("words.out");
        return topology.nativeTopology().describe().toString();
    }

    private String branchSubtypeTopologyDescription() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        var topology = test.streams().from(IncomingOrder.class)
                .branch(IncomingOrder.class)
                .to("orders.out");
        return topology.nativeTopology().describe().toString();
    }

    private String mergeTopologyDescription() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        var streams = test.streams();
        var source = streams.from(IncomingOrder.class);
        var topology = source.filter(o -> o.customer().id().startsWith("A"))
                .merge(source.filter(o -> o.customer().id().startsWith("B")))
                .to(IncomingOrder.class);
        return topology.nativeTopology().describe().toString();
    }

    private String joinTopologyDescription() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        var streams = test.streams();
        var topology = streams.from(IncomingOrder.class)
                .join(
                        streams.from(ShippingUpdate.class),
                        JoinWindow.of(Duration.ofSeconds(10), Duration.ofSeconds(1)),
                        (order, shipping) -> new JoinedOrder(order.orderId(), order.customer(), shipping.status())
                )
                .to(JoinedOrder.class);
        return topology.nativeTopology().describe().toString();
    }

    private String processTopologyDescription() {
        var test = KafkaTopologyTestBootstrap.bootstrap();
        var topology = test.streams().from(IncomingOrder.class)
                .process(new CountOrderProcessor())
                .to("prefab-streams-test-order-count");
        return topology.nativeTopology().describe().toString();
    }
}
