package be.appify.prefab.example.streams.meter;

import be.appify.prefab.core.domain.Keyed;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record MeterDataIngestionState(
    MeterDataIngestionKey key,
    List<Range<Instant>> ingestedRanges,
    int numberOfIngestedValues,
    int totalNumberOfValues
) implements Keyed<MeterDataIngestionKey> {
    public static MeterDataIngestionState from(MeterDataIngestionKey key, MeterData meterData, int totalNumberOfValues) {
        return new MeterDataIngestionState(
                key,
                List.of(rangeOf(meterData)),
                meterData.values().size(),
                totalNumberOfValues
        );
    }

    private static Range<Instant> rangeOf(MeterData meterData) {
        return new Range<>(meterData.start(), meterData.start().plus(meterData.interval().multipliedBy(meterData.values().size() - 1)));
    }

    public MeterDataIngestionState add(MeterData rawMeterData) {
        var ranges = new ArrayList<>(ingestedRanges);
        ranges.add(rangeOf(rawMeterData));
        return new MeterDataIngestionState(
                key,
                ranges,
                numberOfIngestedValues + rawMeterData.values().size(),
                totalNumberOfValues
        );
    }
}
