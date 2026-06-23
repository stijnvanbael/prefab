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
                    SchemaSupport.getString(genericRecord, "id"),
                    SchemaSupport.getRecord(genericRecord, "totalAmount", genericRecordToNestedRecordEventMoneyConverter::convert),
                    SchemaSupport.getRecord(genericRecord, "paidAmount", genericRecordToNestedRecordEventMoneyConverter::convert)
                );
    }
}
