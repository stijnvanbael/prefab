package event.avro.infrastructure.avro;

import event.avro.DeepNestedRecordEvent;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class DeepNestedRecordEventOrderAddressToGenericRecordConverter implements Converter<DeepNestedRecordEvent.Order.Address, GenericRecord> {
    private final Schema schema;

    public DeepNestedRecordEventOrderAddressToGenericRecordConverter(
            DeepNestedRecordEventOrderAddressSchemaFactory schemaFactory) {
        this.schema = schemaFactory.createSchema();
    }

    @Override
    public GenericRecord convert(DeepNestedRecordEvent.Order.Address event) {
        var genericRecord = new GenericData.Record(schema);
        genericRecord.put("street", event.street());
        genericRecord.put("city", event.city());
        return genericRecord;
    }
}
