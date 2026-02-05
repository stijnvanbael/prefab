package event.avro.infrastructure.avro;

import event.avro.SimpleEvent;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class SimpleEventToGenericRecordConverter implements Converter<SimpleEvent, GenericRecord> {
    private final Schema schema;

    public SimpleEventToGenericRecordConverter(SimpleEventSchemaFactory schemaFactory) {
        this.schema = schemaFactory.createSchema();
    }

    @Override
    public GenericRecord convert(SimpleEvent event) {
        var genericRecord = new GenericData.Record(schema);
        genericRecord.put("name", event.name());
        genericRecord.put("age", event.age());
        genericRecord.put("score", event.score());
        genericRecord.put("active", event.active());
        return genericRecord;
    }
}
