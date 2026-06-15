package be.appify.prefab.example.streams;

import be.appify.prefab.core.domain.Key;

import java.time.LocalDate;

import static java.time.ZoneOffset.UTC;

public record ProductionKey(BiddingZoneId biddingZoneId, LocalDate date) implements Key<ProductionKey> {
    public static ProductionKey of(RawProductionData rawProduction) {
        return new ProductionKey(rawProduction.biddingZone(), rawProduction.start().atZone(UTC).toLocalDate());
    }
}
