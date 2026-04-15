package event.avro.infrastructure.avro;

import event.avro.SingleValuedNestedEvent;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class GenericRecordToSingleValuedNestedEventConverter implements Converter<GenericRecord, SingleValuedNestedEvent> {
    private final GenericRecordToSingleValuedNestedEventOuterConverter genericRecordToSingleValuedNestedEventOuterConverter;

    public GenericRecordToSingleValuedNestedEventConverter(
            GenericRecordToSingleValuedNestedEventOuterConverter genericRecordToSingleValuedNestedEventOuterConverter) {
        this.genericRecordToSingleValuedNestedEventOuterConverter = genericRecordToSingleValuedNestedEventOuterConverter;
    }

    @Override
    public SingleValuedNestedEvent convert(GenericRecord genericRecord) {
        return new SingleValuedNestedEvent(
                    genericRecord.get("id").toString(),
                    genericRecord.get("outer") != null ? genericRecordToSingleValuedNestedEventOuterConverter.convert((GenericRecord) genericRecord.get("outer")) : null
                );
    }
}
