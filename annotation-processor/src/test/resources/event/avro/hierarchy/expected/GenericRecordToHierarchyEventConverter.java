package event.avro.infrastructure.avro;

import event.avro.HierarchyEvent;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class GenericRecordToHierarchyEventConverter implements Converter<GenericRecord, HierarchyEvent> {
    private final GenericRecordToHierarchyEventCreatedConverter genericRecordToHierarchyEventCreatedConverter;

    private final GenericRecordToHierarchyEventUpdatedConverter genericRecordToHierarchyEventUpdatedConverter;

    public GenericRecordToHierarchyEventConverter(
            GenericRecordToHierarchyEventCreatedConverter genericRecordToHierarchyEventCreatedConverter,
            GenericRecordToHierarchyEventUpdatedConverter genericRecordToHierarchyEventUpdatedConverter) {
        this.genericRecordToHierarchyEventCreatedConverter = genericRecordToHierarchyEventCreatedConverter;
        this.genericRecordToHierarchyEventUpdatedConverter = genericRecordToHierarchyEventUpdatedConverter;
    }

    @Override
    public HierarchyEvent convert(GenericRecord genericRecord) {
        return switch(genericRecord.getSchema().getName()) {
                    case "HierarchyEvent.Created" -> genericRecordToHierarchyEventCreatedConverter.convert(genericRecord);
                    case "HierarchyEvent.Updated" -> genericRecordToHierarchyEventUpdatedConverter.convert(genericRecord);
                    default -> throw new IllegalArgumentException("Unknown subtype: " + genericRecord.getSchema().getName());
                };
    }
}
