package event.avro.infrastructure.avro;

import event.avro.NullableEvent;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class NullableEventToGenericRecordConverter implements Converter<NullableEvent, GenericRecord> {
    private final Schema schema;

    public NullableEventToGenericRecordConverter(NullableEventSchemaFactory schemaFactory) {
        this.schema = schemaFactory.createSchema();
    }

    @Override
    public GenericRecord convert(NullableEvent event) {
        var genericRecord = new GenericData.Record(schema);
        genericRecord.put("id", event.id());
        genericRecord.put("name", event.name());
        genericRecord.put("description", event.description());
        return genericRecord;
    }
}
