package be.appify.prefab.example.streams.meter;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Keyed;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Event(topic = "raw-meter-data", serialization = Event.Serialization.JSON)
public record RawMeterData(
        MeterSerialNumber meterSerialNumber,
        String filename,
        Instant fileTimestamp,
        Instant start,
        Duration interval,
        List<Double> values,
        int totalNumberOfValues
) implements Keyed<MeterSerialNumber> {
    public MeterData toMeterData() {
        return new MeterData(
                meterSerialNumber,
                start,
                interval,
                values
        );
    }

    @Override
    public MeterSerialNumber key() {
        return meterSerialNumber;
    }
}
