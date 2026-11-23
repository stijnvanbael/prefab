package be.appify.prefab.example.showcase.streams;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.showcase.ordering.Order;
import be.appify.prefab.example.showcase.ordering.OrderEvent;
import be.appify.prefab.streams.AggregationWindow;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.stream.IntStream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Builds order revenue analytics enriched by the replicated catalog. */
@Configuration
class OrderSalesAnalyticsConfiguration {
    static final Duration HOURLY_GRACE = Duration.ofMinutes(5);

    @Bean
    StreamDefinition orderSalesAnalytics(PrefabStreams streams) {
        var catalog = streams.globalStore(CatalogProduct.class);
        var salesByCategory = streams.from(OrderEvent.class)
                .branch(OrderEvent.Placed.class)
                .filter(order -> order.quantity() > 0 && order.total().signum() > 0)
                .flatMap(OrderSalesAnalyticsConfiguration::lines)
                .process(new CatalogEnrichmentProcessor(catalog))
                .groupBy(String.class, CategorySale::categoryId);

        salesByCategory.windowedAggregate(
                        AggregationWindow.tumbling(Duration.ofHours(1), HOURLY_GRACE),
                        HourlyCategoryRevenue.class,
                        HourlyCategoryRevenue::empty,
                        (sale, total) -> total.add(sale))
                .to(HourlyCategoryRevenue.class);

        return salesByCategory
                .aggregate(CategoryRevenue.class, CategoryRevenue::empty, (sale, total) -> total.add(sale))
                .to(CategoryRevenue.class);
    }

    private static Iterable<OrderLine> lines(OrderEvent.Placed order) {
        var unitRevenue = order.total().divide(BigDecimal.valueOf(order.quantity()));
        return IntStream.range(0, order.quantity())
                .mapToObj(ignored -> new OrderLine(order.order(), order.product().id(), unitRevenue))
                .toList();
    }
}
