package be.appify.prefab.example.streams;

import java.time.LocalDate;

import static java.time.ZoneOffset.UTC;

public record ProductionKey(BiddingZoneId biddingZoneId, LocalDate date) {
    public static ProductionKey of(RawProductionData rawProduction) {
        return new ProductionKey(rawProduction.biddingZone(), rawProduction.start().atZone(UTC).toLocalDate());
    }
}
