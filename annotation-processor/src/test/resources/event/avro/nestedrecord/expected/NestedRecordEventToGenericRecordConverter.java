package event.avro.infrastructure.avro;

import event.avro.NestedRecordEvent;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class NestedRecordEventToGenericRecordConverter implements Converter<NestedRecordEvent, GenericRecord> {
    private final Schema schema;

    private final NestedRecordEventMoneyToGenericRecordConverter nestedRecordEventMoneyToGenericRecordConverter;

    public NestedRecordEventToGenericRecordConverter(NestedRecordEventSchemaFactory schemaFactory,
            NestedRecordEventMoneyToGenericRecordConverter nestedRecordEventMoneyToGenericRecordConverter) {
        this.schema = schemaFactory.createSchema();
        this.nestedRecordEventMoneyToGenericRecordConverter = nestedRecordEventMoneyToGenericRecordConverter;
    }

    @Override
    public GenericRecord convert(NestedRecordEvent event) {
        var genericRecord = new GenericData.Record(schema);
        genericRecord.put("id", event.id());
        genericRecord.put("totalAmount", event.totalAmount() != null ? nestedRecordEventMoneyToGenericRecordConverter.convert(event.totalAmount()) : null);
        genericRecord.put("paidAmount", event.paidAmount() != null ? nestedRecordEventMoneyToGenericRecordConverter.convert(event.paidAmount()) : null);
        return genericRecord;
    }
}
