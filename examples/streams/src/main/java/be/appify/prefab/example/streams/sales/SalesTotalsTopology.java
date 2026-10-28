package be.appify.prefab.example.streams.sales;

import be.appify.prefab.example.streams.BiddingZoneId;
import be.appify.prefab.streams.AggregationWindow;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Maintains running sales totals per bidding zone: over all time (non-windowed aggregate) and per hour
 * (windowed aggregate).
 */
@Configuration
public class SalesTotalsTopology {
    static final Duration HOURLY_GRACE = Duration.ofMinutes(5);

    @Bean
    public StreamDefinition salesTotalsTopology(PrefabStreams streams) {
        return salesTotalsTopology(streams, HOURLY_GRACE);
    }

    /**
     * Builds the topology with a configurable grace period for the hourly totals: sales arriving later than
     * {@code grace} after their hour has ended are dropped from the hourly totals.
     */
    StreamDefinition salesTotalsTopology(PrefabStreams streams, Duration grace) {
        var salesPerZone = streams.from(EnergySale.class)
                .groupBy(BiddingZoneId.class, EnergySale::biddingZone);
        salesPerZone
                .windowedAggregate(
                        AggregationWindow.tumbling(Duration.ofHours(1), grace),
                        HourlyZoneSalesTotal.class,
                        HourlyZoneSalesTotal::empty,
                        (sale, total) -> total.add(sale))
                .to(HourlyZoneSalesTotal.class);
        return salesPerZone
                .aggregate(ZoneSalesTotal.class, ZoneSalesTotal::empty, (sale, total) -> total.add(sale))
                .to(ZoneSalesTotal.class);
    }
}
