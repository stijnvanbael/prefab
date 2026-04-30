package event.avro.infrastructure.avro;

import event.avro.SingleValuedNestedEvent;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component("event_avro_SingleValuedNestedEventToGenericRecordConverter")
public class SingleValuedNestedEventToGenericRecordConverter implements Converter<SingleValuedNestedEvent, GenericRecord> {
    private final Schema schema;

    private final SingleValuedNestedEventOuterToGenericRecordConverter singleValuedNestedEventOuterToGenericRecordConverter;

    public SingleValuedNestedEventToGenericRecordConverter(
            SingleValuedNestedEventSchemaFactory schemaFactory,
            SingleValuedNestedEventOuterToGenericRecordConverter singleValuedNestedEventOuterToGenericRecordConverter) {
        this.schema = schemaFactory.createSchema();
        this.singleValuedNestedEventOuterToGenericRecordConverter = singleValuedNestedEventOuterToGenericRecordConverter;
    }

    @Override
    public GenericRecord convert(SingleValuedNestedEvent event) {
        var genericRecord = new GenericData.Record(schema);
        genericRecord.put("id", event.id());
        genericRecord.put("outer", event.outer() != null ? singleValuedNestedEventOuterToGenericRecordConverter.convert(event.outer()) : null);
        return genericRecord;
    }
}
