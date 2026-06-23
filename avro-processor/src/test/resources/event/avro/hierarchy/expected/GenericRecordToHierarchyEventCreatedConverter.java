package event.avro.infrastructure.avro;

import be.appify.prefab.avro.SchemaSupport;
import event.avro.HierarchyEvent;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component("event_avro_GenericRecordToHierarchyEventCreatedConverter")
public class GenericRecordToHierarchyEventCreatedConverter implements Converter<GenericRecord, HierarchyEvent.Created> {
    public GenericRecordToHierarchyEventCreatedConverter() {
    }

    @Override
    public HierarchyEvent.Created convert(GenericRecord genericRecord) {
        return new HierarchyEvent.Created(
                    SchemaSupport.getField(genericRecord, "id").toString(),
                    SchemaSupport.getField(genericRecord, "name").toString()
                );
    }
}
