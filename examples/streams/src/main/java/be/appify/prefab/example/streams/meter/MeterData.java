package be.appify.prefab.example.streams.meter;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.domain.Keyed;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Event(topic = "meter-data", serialization = Event.Serialization.JSON)
public record MeterData(
        MeterSerialNumber key,
        Instant start,
        Duration interval,
        List<Double> values
) implements Keyed<MeterSerialNumber> {
    public MeterData clip(List<Range<Instant>> ranges) {
        var clippedValues = values.stream()
                .filter(value -> ranges.stream()
                        .anyMatch(range -> !range.contains(start.plus(interval.multipliedBy(values.indexOf(value))))))
                .toList();
        return new MeterData(
                key,
                start,
                interval,
                clippedValues
        );
    }
}
