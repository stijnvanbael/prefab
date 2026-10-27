package be.appify.prefab.example.streams.sales;

import be.appify.prefab.example.streams.BiddingZoneId;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Maintains a running total of energy sales per bidding zone using a non-windowed aggregate. */
@Configuration
public class SalesTotalsTopology {
    @Bean
    public StreamDefinition salesTotalsTopology(PrefabStreams streams) {
        return streams.from(EnergySale.class)
                .groupBy(BiddingZoneId.class, EnergySale::biddingZone)
                .aggregate(ZoneSalesTotal.class, ZoneSalesTotal::empty, (sale, total) -> total.add(sale))
                .to(ZoneSalesTotal.class);
    }
}
