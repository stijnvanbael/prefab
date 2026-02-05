package event.avro.infrastructure.avro;

import event.avro.HierarchyEvent;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class GenericRecordToHierarchyEventUpdatedConverter implements Converter<GenericRecord, HierarchyEvent.Updated> {
    public GenericRecordToHierarchyEventUpdatedConverter() {
    }

    @Override
    public HierarchyEvent.Updated convert(GenericRecord genericRecord) {
        return new HierarchyEvent.Updated(
                    genericRecord.get("id").toString(),
                    genericRecord.get("name").toString()
                );
    }
}
