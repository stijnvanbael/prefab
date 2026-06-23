package event.avro.infrastructure.avro;

import be.appify.prefab.avro.SchemaSupport;
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
                    SchemaSupport.getArray(genericRecord, "tags", item -> item.toString()),
                    SchemaSupport.getArray(genericRecord, "lines", item -> item != null ? genericRecordToArrayFieldEventSaleLineConverter.convert((GenericRecord) item) : null)
                );
    }
}
