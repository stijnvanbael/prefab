package be.appify.prefab.example.streams.meter;

import be.appify.prefab.streams.StatefulStreamProcessor;
import be.appify.prefab.streams.StreamRecord;

public class RawMeterDataProcessor extends StatefulStreamProcessor<MeterSerialNumber, RawMeterData, MeterSerialNumber, MeterData> {
    protected RawMeterDataProcessor() {
        super(MeterDataIngestionState.class);
    }

    @Override
    public void process(StreamRecord<MeterSerialNumber, RawMeterData> streamRecord) {
        var rawMeterData = streamRecord.value();
        var key = new MeterDataIngestionKey(
                rawMeterData.meterSerialNumber(),
                rawMeterData.filename(),
                rawMeterData.fileTimestamp()
        );
        store(MeterDataIngestionState.class).putOrUpdate(
                key,
                () -> {
                    var meterData = rawMeterData.toMeterData();
                    var status = MeterDataIngestionState.from(key, meterData, rawMeterData.totalNumberOfValues());
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
