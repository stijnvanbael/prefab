package event.avro.infrastructure.avro;

import event.avro.NestedRecordEvent;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class GenericRecordToNestedRecordEventConverter implements Converter<GenericRecord, NestedRecordEvent> {
    private final GenericRecordToNestedRecordEventMoneyConverter genericRecordToNestedRecordEventMoneyConverter;

    public GenericRecordToNestedRecordEventConverter(
            GenericRecordToNestedRecordEventMoneyConverter genericRecordToNestedRecordEventMoneyConverter) {
        this.genericRecordToNestedRecordEventMoneyConverter = genericRecordToNestedRecordEventMoneyConverter;
    }

    @Override
    public NestedRecordEvent convert(GenericRecord genericRecord) {
        return new NestedRecordEvent(
                    genericRecord.get("id").toString(),
                    genericRecord.get("totalAmount") != null ? genericRecordToNestedRecordEventMoneyConverter.convert((GenericRecord) genericRecord.get("totalAmount")) : null,
                    genericRecord.get("paidAmount") != null ? genericRecordToNestedRecordEventMoneyConverter.convert((GenericRecord) genericRecord.get("paidAmount")) : null
                );
    }
}
