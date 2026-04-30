package event.avro.infrastructure.avro;

import event.avro.DeepNestedRecordEvent;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component("event_avro_DeepNestedRecordEventToGenericRecordConverter")
public class DeepNestedRecordEventToGenericRecordConverter implements Converter<DeepNestedRecordEvent, GenericRecord> {
    private final Schema schema;

    private final DeepNestedRecordEventOrderToGenericRecordConverter deepNestedRecordEventOrderToGenericRecordConverter;

    public DeepNestedRecordEventToGenericRecordConverter(
            DeepNestedRecordEventSchemaFactory schemaFactory,
            DeepNestedRecordEventOrderToGenericRecordConverter deepNestedRecordEventOrderToGenericRecordConverter) {
        this.schema = schemaFactory.createSchema();
        this.deepNestedRecordEventOrderToGenericRecordConverter = deepNestedRecordEventOrderToGenericRecordConverter;
    }

    @Override
    public GenericRecord convert(DeepNestedRecordEvent event) {
        var genericRecord = new GenericData.Record(schema);
        genericRecord.put("id", event.id());
        genericRecord.put("order", event.order() != null ? deepNestedRecordEventOrderToGenericRecordConverter.convert(event.order()) : null);
        return genericRecord;
    }
}
