package be.appify.prefab.example.showcase.streams;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.showcase.catalog.Product;
import be.appify.prefab.example.showcase.customer.Customer;
import be.appify.prefab.example.showcase.ordering.Order;
import be.appify.prefab.example.showcase.ordering.OrderEvent;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.testing.TestStreams;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestExtension;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(TopologyTestExtension.class)
class OrderRoutingTopologyTest {
    @TestStreams
    PrefabStreams streams;
    @TestStreams
    TopologySession topology;

    @Test
    void shouldRouteByOrderTypeAndMergeTheBranches() {
        new OrderRoutingConfiguration().orderRouting(streams);

        Reference<Order> order = Reference.fromId("order-1");
        var events = topology.input(OrderEvent.class);
        events.send(new OrderEvent.Placed(
                order,
                Reference.<Customer>fromId("customer-1"),
                Reference.<Product>fromId("product-1"),
                1,
                BigDecimal.TEN
        ));
        events.send(new OrderEvent.Cancelled(
                order,
                Reference.<Customer>fromId("customer-1"),
                Reference.<Product>fromId("product-1"),
                1
        ));

        assertThat(topology.output(OrderRoute.class).values())
                .containsExactly(new OrderRoute(order, "placed"), new OrderRoute(order, "cancelled"));
    }
}
