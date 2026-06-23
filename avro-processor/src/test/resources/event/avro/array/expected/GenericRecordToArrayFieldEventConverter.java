package event.avro.infrastructure.avro;

import be.appify.prefab.avro.SchemaSupport;
import be.appify.prefab.core.util.Streams;
import event.avro.ArrayFieldEvent;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component("event_avro_GenericRecordToArrayFieldEventConverter")
public class GenericRecordToArrayFieldEventConverter implements Converter<GenericRecord, ArrayFieldEvent> {
    private final GenericRecordToArrayFieldEventSaleLineConverter genericRecordToArrayFieldEventSaleLineConverter;

    public GenericRecordToArrayFieldEventConverter(
            GenericRecordToArrayFieldEventSaleLineConverter genericRecordToArrayFieldEventSaleLineConverter) {
        this.genericRecordToArrayFieldEventSaleLineConverter = genericRecordToArrayFieldEventSaleLineConverter;
    }

    @Override
    public ArrayFieldEvent convert(GenericRecord genericRecord) {
        return new ArrayFieldEvent(
                    SchemaSupport.getField(genericRecord, "tags") != null ? Streams.stream(((GenericData.Array<?>) SchemaSupport.getField(genericRecord, "tags")).iterator())
                        .map(item -> item.toString())
                        .toList() : null,
                    SchemaSupport.getField(genericRecord, "lines") != null ? Streams.stream(((GenericData.Array<?>) SchemaSupport.getField(genericRecord, "lines")).iterator())
                        .map(item -> item != null ? genericRecordToArrayFieldEventSaleLineConverter.convert((GenericRecord) item) : null)
                        .toList() : null
                );
    }
}
