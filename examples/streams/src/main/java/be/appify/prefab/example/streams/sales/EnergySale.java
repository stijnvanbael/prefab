package be.appify.prefab.example.streams.sales;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.example.streams.BiddingZoneId;

@Event(topic = "energy-sales")
public record EnergySale(String saleId, BiddingZoneId biddingZone, double amount) implements Keyed<String> {
    @Override
    public String key() {
        return saleId;
    }
}
