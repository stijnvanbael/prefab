package be.appify.prefab.example.streams.meter;

import be.appify.prefab.core.domain.Key;

import java.time.Instant;

public record RawMeterDataKey(
        MeterSerialNumber meterSerialNumber,
        String filename,
        Instant fileTimestamp
) implements Key<RawMeterDataKey> {
    static {
        Key.register(RawMeterDataKey.class, RawMeterDataKey::parse); // TODO: support JSON/AVRO keys
    }

    public static RawMeterDataKey parse(String key) {
        var split = key.split("\\|");
        return new RawMeterDataKey(new MeterSerialNumber(split[0]), split[1], Instant.parse(split[2]));
    }

    public String toString() {
        return "%s|%s|%s".formatted(meterSerialNumber.value(), filename, fileTimestamp);
    }
}
