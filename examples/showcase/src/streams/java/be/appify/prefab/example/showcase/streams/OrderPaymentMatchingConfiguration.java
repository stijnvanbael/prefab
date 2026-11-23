package be.appify.prefab.example.showcase.streams;

import be.appify.prefab.example.showcase.ordering.OrderEvent;
import be.appify.prefab.streams.JoinWindow;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Matches placed orders to payment confirmations arriving nearby in event time. */
@Configuration
class OrderPaymentMatchingConfiguration {
    @Bean
    StreamDefinition orderPaymentMatching(PrefabStreams streams) {
        var orders = streams.from(OrderEvent.class)
                .branch(OrderEvent.Placed.class)
                .map(order -> new PaymentJoinInput(order.order(), order.total()));
        var payments = streams.from(OrderPayment.class)
                .map(payment -> new PaymentJoinInput(payment.order(), payment.amount()));

        return orders.join(
                        payments,
                        JoinWindow.of(Duration.ofMinutes(5), Duration.ofMinutes(1)),
                        (order, payment) -> new MatchedOrderPayment(
                                order.order(), order.amount(), payment.amount()))
                .to(MatchedOrderPayment.class);
    }
}
