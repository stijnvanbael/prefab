package event.avro.infrastructure.avro;

import event.avro.DeepNestedRecordEvent;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component("event_avro_GenericRecordToDeepNestedRecordEventOrderConverter")
public class GenericRecordToDeepNestedRecordEventOrderConverter implements Converter<GenericRecord, DeepNestedRecordEvent.Order> {
    private final GenericRecordToDeepNestedRecordEventOrderAddressConverter genericRecordToDeepNestedRecordEventOrderAddressConverter;

    public GenericRecordToDeepNestedRecordEventOrderConverter(
            GenericRecordToDeepNestedRecordEventOrderAddressConverter genericRecordToDeepNestedRecordEventOrderAddressConverter) {
        this.genericRecordToDeepNestedRecordEventOrderAddressConverter = genericRecordToDeepNestedRecordEventOrderAddressConverter;
    }

    @Override
    public DeepNestedRecordEvent.Order convert(GenericRecord genericRecord) {
        return new DeepNestedRecordEvent.Order(
                    genericRecord.get("orderId").toString(),
                    genericRecord.get("shippingAddress") != null ? genericRecordToDeepNestedRecordEventOrderAddressConverter.convert((GenericRecord) genericRecord.get("shippingAddress")) : null
                );
    }
}
