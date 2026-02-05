package event.avro.infrastructure.avro;

import event.avro.InheritEvent;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class InheritEventToGenericRecordConverter implements Converter<InheritEvent, GenericRecord> {
    private final Schema schema;

    public InheritEventToGenericRecordConverter(InheritEventSchemaFactory schemaFactory) {
        this.schema = schemaFactory.createSchema();
    }

    @Override
    public GenericRecord convert(InheritEvent event) {
        var genericRecord = new GenericData.Record(schema);
        genericRecord.put("superField", event.superField());
        genericRecord.put("subField", event.subField());
        return genericRecord;
    }
}
