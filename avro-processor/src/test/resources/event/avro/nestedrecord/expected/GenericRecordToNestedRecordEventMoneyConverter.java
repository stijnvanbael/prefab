package event.avro.infrastructure.avro;

import be.appify.prefab.avro.SchemaSupport;
import event.avro.NestedRecordEvent;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component("event_avro_GenericRecordToNestedRecordEventMoneyConverter")
public class GenericRecordToNestedRecordEventMoneyConverter implements Converter<GenericRecord, NestedRecordEvent.Money> {
    public GenericRecordToNestedRecordEventMoneyConverter() {
    }

    @Override
    public NestedRecordEvent.Money convert(GenericRecord genericRecord) {
        return new NestedRecordEvent.Money(
                    SchemaSupport.getString(genericRecord, "currency"),
                    SchemaSupport.getDouble(genericRecord, "amount")
                );
    }
}
