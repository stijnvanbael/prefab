package be.appify.prefab.example.streams;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Keyed;

import java.time.Instant;
import java.util.List;

@Event(topic = "raw-production")
public record RawProductionData(
        BiddingZoneId biddingZone,
        EnergySource source,
        Instant start,
        List<Double> values
) implements Keyed<BiddingZoneId> {
    @Override
    public BiddingZoneId key() {
        return biddingZone;
    }
}
