package be.appify.prefab.example.showcase.streams;

import be.appify.prefab.example.showcase.ordering.OrderEvent;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Routes placed and cancelled orders separately, then merges them back into one typed stream. */
@Configuration
class OrderRoutingConfiguration {
    @Bean
    StreamDefinition orderRouting(PrefabStreams streams) {
        var events = streams.from(OrderEvent.class);
        var placed = events.branch(OrderEvent.Placed.class)
                .map(order -> new OrderRoute(order.order(), "placed"));
        var cancelled = events.branch(OrderEvent.Cancelled.class)
                .map(order -> new OrderRoute(order.order(), "cancelled"));
        return streams.merge(placed, cancelled).to(OrderRoute.class);
    }
}
