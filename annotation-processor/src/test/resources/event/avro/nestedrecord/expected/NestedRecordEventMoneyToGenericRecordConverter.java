package event.avro.infrastructure.avro;

import event.avro.NestedRecordEvent;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class NestedRecordEventMoneyToGenericRecordConverter implements Converter<NestedRecordEvent.Money, GenericRecord> {
    private final Schema schema;

    public NestedRecordEventMoneyToGenericRecordConverter(
            NestedRecordEventMoneySchemaFactory schemaFactory) {
        this.schema = schemaFactory.createSchema();
    }

    @Override
    public GenericRecord convert(NestedRecordEvent.Money event) {
        var genericRecord = new GenericData.Record(schema);
        genericRecord.put("currency", event.currency());
        genericRecord.put("amount", event.amount());
        return genericRecord;
    }
}
