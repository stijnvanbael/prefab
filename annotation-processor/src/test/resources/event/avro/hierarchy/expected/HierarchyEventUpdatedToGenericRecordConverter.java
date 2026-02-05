package event.avro.infrastructure.avro;

import event.avro.HierarchyEvent;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class HierarchyEventUpdatedToGenericRecordConverter implements Converter<HierarchyEvent.Updated, GenericRecord> {
    private final Schema schema;

    public HierarchyEventUpdatedToGenericRecordConverter(
            HierarchyEventUpdatedSchemaFactory schemaFactory) {
        this.schema = schemaFactory.createSchema();
    }

    @Override
    public GenericRecord convert(HierarchyEvent.Updated event) {
        var genericRecord = new GenericData.Record(schema);
        genericRecord.put("id", event.id());
        genericRecord.put("name", event.name());
        return genericRecord;
    }
}
