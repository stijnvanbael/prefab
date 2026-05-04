package event.avro.infrastructure.avro;

import event.avro.SingleValuedNestedEvent;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component("event_avro_GenericRecordToSingleValuedNestedEventOuterConverter")
public class GenericRecordToSingleValuedNestedEventOuterConverter implements Converter<GenericRecord, SingleValuedNestedEvent.Outer> {
    public GenericRecordToSingleValuedNestedEventOuterConverter() {
    }

    @Override
    public SingleValuedNestedEvent.Outer convert(GenericRecord genericRecord) {
        return new SingleValuedNestedEvent.Outer(
                    genericRecord.get("inner") != null ? genericRecord.get("inner") instanceof GenericRecord singleValueRecord
                        ? new SingleValuedNestedEvent.Outer.Inner(singleValueRecord.get("value").toString())
                        : new SingleValuedNestedEvent.Outer.Inner(genericRecord.get("inner").toString()) : null
                );
    }
}
