package event.avro.infrastructure.avro;

import be.appify.prefab.avro.SchemaSupport;
import event.avro.NestedRecordEvent;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component("event_avro_GenericRecordToNestedRecordEventConverter")
public class GenericRecordToNestedRecordEventConverter implements Converter<GenericRecord, NestedRecordEvent> {
    private final GenericRecordToNestedRecordEventMoneyConverter genericRecordToNestedRecordEventMoneyConverter;

    public GenericRecordToNestedRecordEventConverter(
            GenericRecordToNestedRecordEventMoneyConverter genericRecordToNestedRecordEventMoneyConverter) {
        this.genericRecordToNestedRecordEventMoneyConverter = genericRecordToNestedRecordEventMoneyConverter;
    }

    @Override
    public NestedRecordEvent convert(GenericRecord genericRecord) {
        return new NestedRecordEvent(
                    SchemaSupport.getField(genericRecord, "id").toString(),
                    SchemaSupport.getField(genericRecord, "totalAmount") != null ? genericRecordToNestedRecordEventMoneyConverter.convert((GenericRecord) SchemaSupport.getField(genericRecord, "totalAmount")) : null,
                    SchemaSupport.getField(genericRecord, "paidAmount") != null ? genericRecordToNestedRecordEventMoneyConverter.convert((GenericRecord) SchemaSupport.getField(genericRecord, "paidAmount")) : null
                );
    }
}
