package event.avro.infrastructure.avro;

import event.avro.SingleValuedNestedEvent;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component("event_avro_SingleValuedNestedEventOuterToGenericRecordConverter")
public class SingleValuedNestedEventOuterToGenericRecordConverter implements Converter<SingleValuedNestedEvent.Outer, GenericRecord> {
    private final Schema schema;

    public SingleValuedNestedEventOuterToGenericRecordConverter(
            SingleValuedNestedEventOuterSchemaFactory schemaFactory) {
        this.schema = schemaFactory.createSchema();
    }

    @Override
    public GenericRecord convert(SingleValuedNestedEvent.Outer event) {
        var genericRecord = new GenericData.Record(schema);
        genericRecord.put("inner", event.inner() != null ? event.inner().value() : null);
        return genericRecord;
    }
}
