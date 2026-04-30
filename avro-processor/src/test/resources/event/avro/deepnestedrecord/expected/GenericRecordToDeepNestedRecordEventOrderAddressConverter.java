package event.avro.infrastructure.avro;

import event.avro.DeepNestedRecordEvent;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component("event_avro_GenericRecordToDeepNestedRecordEventOrderAddressConverter")
public class GenericRecordToDeepNestedRecordEventOrderAddressConverter implements Converter<GenericRecord, DeepNestedRecordEvent.Order.Address> {
    public GenericRecordToDeepNestedRecordEventOrderAddressConverter() {
    }

    @Override
    public DeepNestedRecordEvent.Order.Address convert(GenericRecord genericRecord) {
        return new DeepNestedRecordEvent.Order.Address(
                    genericRecord.get("street").toString(),
                    genericRecord.get("city").toString()
                );
    }
}
