package event.avro.infrastructure.avro;

import event.avro.HierarchyEvent;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class HierarchyEventToGenericRecordConverter implements Converter<HierarchyEvent, GenericRecord> {
    private final Schema schema;

    private final HierarchyEventCreatedToGenericRecordConverter hierarchyEventCreatedToGenericRecordConverter;

    private final HierarchyEventUpdatedToGenericRecordConverter hierarchyEventUpdatedToGenericRecordConverter;

    public HierarchyEventToGenericRecordConverter(HierarchyEventSchemaFactory schemaFactory,
            HierarchyEventCreatedToGenericRecordConverter hierarchyEventCreatedToGenericRecordConverter,
            HierarchyEventUpdatedToGenericRecordConverter hierarchyEventUpdatedToGenericRecordConverter) {
        this.schema = schemaFactory.createSchema();
        this.hierarchyEventCreatedToGenericRecordConverter = hierarchyEventCreatedToGenericRecordConverter;
        this.hierarchyEventUpdatedToGenericRecordConverter = hierarchyEventUpdatedToGenericRecordConverter;
    }

    @Override
    public GenericRecord convert(HierarchyEvent event) {
        return switch(event) {
                    case HierarchyEvent.Created v -> hierarchyEventCreatedToGenericRecordConverter.convert(v);
                    case HierarchyEvent.Updated v -> hierarchyEventUpdatedToGenericRecordConverter.convert(v);
                };
    }
}
