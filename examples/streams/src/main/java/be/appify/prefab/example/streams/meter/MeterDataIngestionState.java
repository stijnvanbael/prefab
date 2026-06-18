package be.appify.prefab.example.streams.meter;

import be.appify.prefab.core.domain.Keyed;
import com.google.common.collect.Range;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record MeterDataIngestionState(
    RawMeterDataKey key,
    List<Range<Instant>> ingestedRanges,
    int numberOfIngestedValues,
    int totalNumberOfValues
) implements Keyed<RawMeterDataKey> {
    public static MeterDataIngestionState from(RawMeterDataKey key, MeterData meterData, int totalNumberOfValues) {
        return new MeterDataIngestionState(
                key,
                List.of(rangeOf(meterData)),
                meterData.values().size(),
                totalNumberOfValues
        );
    }

    private static Range<Instant> rangeOf(MeterData meterData) {
        return Range.closed(meterData.start(), meterData.start().plus(meterData.interval().multipliedBy(meterData.values().size() - 1)));
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
