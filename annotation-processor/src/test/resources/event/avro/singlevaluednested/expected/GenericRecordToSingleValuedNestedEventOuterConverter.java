package event.avro.infrastructure.avro;

import event.avro.SingleValuedNestedEvent;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class GenericRecordToSingleValuedNestedEventOuterConverter implements Converter<GenericRecord, SingleValuedNestedEvent.Outer> {
    public GenericRecordToSingleValuedNestedEventOuterConverter() {
    }

    @Override
    public SingleValuedNestedEvent.Outer convert(GenericRecord genericRecord) {
        return new SingleValuedNestedEvent.Outer(
                    genericRecord.get("inner") != null ? new SingleValuedNestedEvent.Outer.Inner(genericRecord.get("inner").toString()) : null
                );
    }
}
