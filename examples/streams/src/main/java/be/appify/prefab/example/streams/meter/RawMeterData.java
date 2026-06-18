package be.appify.prefab.example.streams.meter;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Keyed;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Event(topic = "raw-meter-data", serialization = Event.Serialization.JSON)
public record RawMeterData(
        RawMeterDataKey key, // TODO: maybe meter serial number is sufficient as key, and only use the composite key for the ingestion state
        Instant start,
        Duration interval,
        List<Double> values,
        int totalNumberOfValues
) implements Keyed<RawMeterDataKey> {
    public MeterData toMeterData() {
        return new MeterData(
                key.meterSerialNumber(),
                start,
                interval,
                values
        );
    }
}
