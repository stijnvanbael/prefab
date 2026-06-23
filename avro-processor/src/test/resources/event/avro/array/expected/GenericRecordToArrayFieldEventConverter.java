package event.avro.infrastructure.avro;

import be.appify.prefab.avro.SchemaSupport;
import be.appify.prefab.core.util.Streams;
import event.avro.ArrayFieldEvent;
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
                    SchemaSupport.getArray(genericRecord, "tags") != null ? Streams.stream(SchemaSupport.getArray(genericRecord, "tags").iterator())
                        .map(item -> item.toString())
                        .toList() : null,
                    SchemaSupport.getArray(genericRecord, "lines") != null ? Streams.stream(SchemaSupport.getArray(genericRecord, "lines").iterator())
                        .map(item -> item != null ? genericRecordToArrayFieldEventSaleLineConverter.convert((GenericRecord) item) : null)
                        .toList() : null
                );
    }
}
