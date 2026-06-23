package event.avro.infrastructure.avro;

import be.appify.prefab.avro.SchemaSupport;
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
                    SchemaSupport.getField(genericRecord, "inner") != null ? SchemaSupport.getField(genericRecord, "inner") instanceof GenericRecord singleValueRecord
                        ? new SingleValuedNestedEvent.Outer.Inner(SchemaSupport.getField(singleValueRecord, "value").toString())
                        : new SingleValuedNestedEvent.Outer.Inner(SchemaSupport.getField(genericRecord, "inner").toString()) : null
                );
    }
}
