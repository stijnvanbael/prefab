package event.avro.infrastructure.avro;

import event.avro.HierarchyEvent;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class GenericRecordToHierarchyEventCreatedConverter implements Converter<GenericRecord, HierarchyEvent.Created> {
    public GenericRecordToHierarchyEventCreatedConverter() {
    }

    @Override
    public HierarchyEvent.Created convert(GenericRecord genericRecord) {
        return new HierarchyEvent.Created(
                    genericRecord.get("id").toString(),
                    genericRecord.get("name").toString()
                );
    }
}
