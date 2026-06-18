package be.appify.prefab.example.streams.meter;

import be.appify.prefab.streams.StatefulStreamProcessor;
import be.appify.prefab.streams.StreamRecord;

public class RawMeterDataProcessor extends StatefulStreamProcessor<RawMeterDataKey, RawMeterData, MeterSerialNumber, MeterData> {
    protected RawMeterDataProcessor() {
        super(MeterDataIngestionState.class);
    }

    @Override
    public void process(StreamRecord<RawMeterDataKey, RawMeterData> streamRecord) {
        var rawMeterData = streamRecord.value();
        store(MeterDataIngestionState.class).putOrUpdate(
                streamRecord.key(),
                () -> {
                    var meterData = rawMeterData.toMeterData();
                    var status = MeterDataIngestionState.from(streamRecord.key(), meterData, rawMeterData.totalNumberOfValues());
                    forward(new StreamRecord<>(meterData.key(), meterData, streamRecord.timestamp(), streamRecord.headers()));
                    return status;
                },
                status -> {
                    var meterData = rawMeterData.toMeterData().clip(status.ingestedRanges());
                    status = status.add(meterData);
                    forward(new StreamRecord<>(meterData.key(), meterData, streamRecord.timestamp(), streamRecord.headers()));
                    return status;
                });
    }
}
