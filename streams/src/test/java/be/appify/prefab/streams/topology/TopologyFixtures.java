package be.appify.prefab.streams.topology;

import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.streams.AggregationWindow;
import be.appify.prefab.streams.ContextualStreamProcessor;
import be.appify.prefab.streams.GlobalStore;
import be.appify.prefab.streams.JoinWindow;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StatefulStreamProcessor;
import be.appify.prefab.streams.Store;
import be.appify.prefab.streams.StreamRecord;
import be.appify.prefab.streams.testing.TopologyInput;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/** Records, processors and declaration helpers shared by the topology tests. */
public final class TopologyFixtures {
    private TopologyFixtures() {
    }

    public record IncomingOrder(Reference<Order> orderId, Reference<Customer> customer) implements Keyed<Reference<Order>> {
        @Override
        public Reference<Order> key() {
            return orderId;
        }
    }

    public record ProcessedOrder(Reference<Order> orderId, Reference<Customer> customer) implements Keyed<Reference<Order>> {
        @Override
        public Reference<Order> key() {
            return orderId;
        }
    }

    public record Order() {
    }

    public record Customer() {
    }

    public record WordBatch(Reference<Batch> batchId, String words) implements Keyed<Reference<Batch>> {
        @Override
        public Reference<Batch> key() {
            return batchId;
        }
    }

    public record Batch() {
    }

    public record ShippingUpdate(Reference<Order> orderId, String status) implements Keyed<Reference<Order>> {
        @Override
        public Reference<Order> key() {
            return orderId;
        }
    }

    public record JoinedOrder(Reference<Order> orderId, Reference<Customer> customer,
                       String shippingStatus) implements Keyed<Reference<Order>> {
        @Override
        public Reference<Order> key() {
            return orderId;
        }
    }

    public record OrderCount(Reference<Customer> customer, int numberOfOrders) implements Keyed<Reference<Customer>> {
        @Override
        public Reference<Customer> key() {
            return customer;
        }
    }

    public record CustomerProfile(Reference<Customer> customerId, String tier) implements Keyed<Reference<Customer>> {
        @Override
        public Reference<Customer> key() {
            return customerId;
        }
    }

    public record CustomerProfileUpdated(Reference<Customer> customerId, String tier) implements Keyed<Reference<Customer>> {
        @Override
        public Reference<Customer> key() {
            return customerId;
        }
    }

    public record OrderProfileView(Reference<Order> orderId, Reference<Customer> customer, String tier)
            implements Keyed<Reference<Order>> {
        @Override
        public Reference<Order> key() {
            return orderId;
        }
    }

    public record CompositeOrderKey(String biddingZone, String orderId) {
    }

    public record CompositeIncomingOrder(CompositeOrderKey key, String customer) implements Keyed<CompositeOrderKey> {
    }

    public record CompositeShippingUpdate(CompositeOrderKey key, String status) implements Keyed<CompositeOrderKey> {
    }

    public record CompositeJoinedOrder(CompositeOrderKey key, String customer, String shippingStatus)
            implements Keyed<CompositeOrderKey> {
    }

    public record CompositeOrderCount(CompositeOrderKey key, int count) implements Keyed<CompositeOrderKey> {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    public sealed interface OrderEvent extends Keyed<Reference<Order>> permits OrderEvent.OrderCreated, OrderEvent.OrderShipped {
        @Override
        default Reference<Order> key() {
            return orderId();
        }

        Reference<Order> orderId();

        record OrderCreated(Reference<Order> orderId, Reference<Customer> customer) implements OrderEvent {}

        record OrderShipped(Reference<Order> orderId) implements OrderEvent {}
    }

    public static class CountOrderProcessor
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

    public static final class UpdateCustomerProfileUpdater {
        public static void update(
                StreamRecord<Reference<Customer>, CustomerProfileUpdated> streamRecord,
                Store<Reference<Customer>, CustomerProfile> store
        ) {
            var updates = store.get(streamRecord.key())
                    .map(existing -> Integer.parseInt(existing.tier().replaceAll("\\D", "")) + 1)
                    .orElse(1);
            store.put(
                    streamRecord.key(),
                    new CustomerProfile(streamRecord.key(), "%s (%d updates)".formatted(streamRecord.value().tier(), updates)));
        }
    }
    public static final class EnrichOrderWithProfileProcessor
            extends ContextualStreamProcessor<Reference<Order>, IncomingOrder, Reference<Order>, OrderProfileView> {
        private final GlobalStore<Reference<Customer>, CustomerProfile> customerProfiles;
        private final List<GlobalStore<?, ?>> globalStores;

        public EnrichOrderWithProfileProcessor(
                GlobalStore<Reference<Customer>, CustomerProfile> customerProfiles,
                GlobalStore<?, ?>... otherStores
        ) {
            this.customerProfiles = customerProfiles;
            this.globalStores = Stream.concat(Stream.of(customerProfiles), Stream.of(otherStores)).toList();
        }

        @Override
        public Collection<GlobalStore<?, ?>> globalStores() {
            return globalStores;
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

    public static class CountCompositeOrderProcessor
            extends StatefulStreamProcessor<CompositeOrderKey, CompositeIncomingOrder, CompositeOrderKey, CompositeOrderCount> {

        public CountCompositeOrderProcessor() {
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

    public static Reference<Order> orderKey(String id) {
        return Reference.fromId(id);
    }

    public static void pipeOrder(
            TopologyInput<Reference<Order>, IncomingOrder> input,
            String orderId,
            String customer,
            long timestampMillis
    ) {
        input.send(
                new IncomingOrder(Reference.fromId(orderId), Reference.fromId(customer)),
                Instant.ofEpochMilli(timestampMillis));
    }

    @SuppressWarnings("unchecked")
    public static Class<Reference<Customer>> customerKeyType() {
        return (Class<Reference<Customer>>) (Class<?>) Reference.class;
    }

    public static void countOrdersPerCustomer(PrefabStreams streams, AggregationWindow window) {
        streams.from(IncomingOrder.class)
                .groupBy(customerKeyType(), IncomingOrder::customer)
                .windowedAggregate(
                        window,
                        OrderCount.class,
                        () -> new OrderCount(null, 0),
                        (order, count) -> new OrderCount(order.customer(), count.numberOfOrders() + 1)
                )
                .to(OrderCount.class);
    }

    public static void joinOrdersWithShipping(PrefabStreams streams, Duration grace) {
        streams.from(IncomingOrder.class)
                .join(
                        streams.from(ShippingUpdate.class),
                        JoinWindow.of(Duration.ofSeconds(10), grace),
                        (order, shipping) -> new JoinedOrder(order.orderId(), order.customer(), shipping.status())
                )
                .to(JoinedOrder.class);
    }
}
