package be.appify.prefab.example.streams.meter;

import be.appify.prefab.core.domain.Key;

import java.time.Instant;

public record RawMeterDataKey(
        MeterSerialNumber meterSerialNumber,
        String filename,
        Instant fileTimestamp
) implements Key<RawMeterDataKey> {
}
