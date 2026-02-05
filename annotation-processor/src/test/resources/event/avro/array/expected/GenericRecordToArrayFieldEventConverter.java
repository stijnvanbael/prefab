package event.avro.infrastructure.avro;

import be.appify.prefab.core.util.Streams;
import event.avro.ArrayFieldEvent;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class GenericRecordToArrayFieldEventConverter implements Converter<GenericRecord, ArrayFieldEvent> {
    private final GenericRecordToArrayFieldEventSaleLineConverter genericRecordToArrayFieldEventSaleLineConverter;

    public GenericRecordToArrayFieldEventConverter(
            GenericRecordToArrayFieldEventSaleLineConverter genericRecordToArrayFieldEventSaleLineConverter) {
        this.genericRecordToArrayFieldEventSaleLineConverter = genericRecordToArrayFieldEventSaleLineConverter;
    }

    @Override
    public ArrayFieldEvent convert(GenericRecord genericRecord) {
        return new ArrayFieldEvent(
                    genericRecord.get("tags") != null ? Streams.stream(((GenericData.Array<?>) genericRecord.get("tags")).iterator())
                        .map(item -> item.toString())
                        .toList() : null,
                    genericRecord.get("lines") != null ? Streams.stream(((GenericData.Array<?>) genericRecord.get("lines")).iterator())
                        .map(item -> item != null ? genericRecordToArrayFieldEventSaleLineConverter.convert((GenericRecord) item) : null)
                        .toList() : null
                );
    }
}
