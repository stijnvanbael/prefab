package event.avro.infrastructure.avro;

import be.appify.prefab.avro.SchemaSupport;
import event.avro.NonPrimitiveEvent;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component("event_avro_NonPrimitiveEventToGenericRecordConverter")
public class NonPrimitiveEventToGenericRecordConverter implements Converter<NonPrimitiveEvent, GenericRecord> {
    private final Schema schema;

    public NonPrimitiveEventToGenericRecordConverter(NonPrimitiveEventSchemaFactory schemaFactory) {
        this.schema = schemaFactory.createSchema();
    }

    @Override
    public GenericRecord convert(NonPrimitiveEvent event) {
        var genericRecord = new GenericData.Record(schema);
        genericRecord.put("status", event.status() != null ? new GenericData.EnumSymbol(SchemaSupport.enumSchemaOf(schema.getField("status").schema()), event.status().name()) : null);
        genericRecord.put("timestamp", event.timestamp() != null ? event.timestamp().toEpochMilli() : null);
        genericRecord.put("date", event.date() != null ? (int) event.date().toEpochDay() : null);
        genericRecord.put("duration", event.duration() != null ? event.duration().toMillis() : null);
        genericRecord.put("reference", event.reference() != null ? SchemaSupport.isRecordSchema(schema.getField("reference").schema()) ? SchemaSupport.singleValueRecord(schema.getField("reference").schema(), "id", event.reference().id()) : event.reference().id() : null);
        return genericRecord;
    }
}
