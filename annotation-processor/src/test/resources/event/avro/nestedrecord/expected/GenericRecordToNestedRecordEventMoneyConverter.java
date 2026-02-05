package event.avro.infrastructure.avro;

import event.avro.NestedRecordEvent;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class GenericRecordToNestedRecordEventMoneyConverter implements Converter<GenericRecord, NestedRecordEvent.Money> {
    public GenericRecordToNestedRecordEventMoneyConverter() {
    }

    @Override
    public NestedRecordEvent.Money convert(GenericRecord genericRecord) {
        return new NestedRecordEvent.Money(
                    genericRecord.get("currency").toString(),
                    (Double) genericRecord.get("amount")
                );
    }
}
