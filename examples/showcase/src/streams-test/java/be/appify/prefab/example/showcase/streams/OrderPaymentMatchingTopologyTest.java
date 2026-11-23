package be.appify.prefab.example.showcase.streams;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.showcase.ordering.OrderEvent;
import be.appify.prefab.example.showcase.ordering.Order;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.testing.TestStreams;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestExtension;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(TopologyTestExtension.class)
class OrderPaymentMatchingTopologyTest {
    @TestStreams
    PrefabStreams streams;
    @TestStreams
    TopologySession topology;

    @Test
    void shouldMatchPaymentToOrderWithinTheJoinWindow() {
        new OrderPaymentMatchingConfiguration().orderPaymentMatching(streams);

        Reference<Order> order = Reference.fromId("order-1");
        topology.input(OrderEvent.class).send(new OrderEvent.Placed(
                order,
                Reference.fromId("customer-1"),
                Reference.fromId("product-1"),
                1,
                new BigDecimal("42.00")
        ));
        topology.input(OrderPayment.class).send(new OrderPayment(order, new BigDecimal("42.00")));

        assertThat(topology.output(MatchedOrderPayment.class).values())
                .containsExactly(new MatchedOrderPayment(order, new BigDecimal("42.00"), new BigDecimal("42.00")));
    }
}
