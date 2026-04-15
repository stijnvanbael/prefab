package event.avro.infrastructure.avro;

import event.avro.DeepNestedRecordEvent;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class DeepNestedRecordEventOrderToGenericRecordConverter implements Converter<DeepNestedRecordEvent.Order, GenericRecord> {
    private final Schema schema;

    private final DeepNestedRecordEventOrderAddressToGenericRecordConverter deepNestedRecordEventOrderAddressToGenericRecordConverter;

    public DeepNestedRecordEventOrderToGenericRecordConverter(
            DeepNestedRecordEventOrderSchemaFactory schemaFactory,
            DeepNestedRecordEventOrderAddressToGenericRecordConverter deepNestedRecordEventOrderAddressToGenericRecordConverter) {
        this.schema = schemaFactory.createSchema();
        this.deepNestedRecordEventOrderAddressToGenericRecordConverter = deepNestedRecordEventOrderAddressToGenericRecordConverter;
    }

    @Override
    public GenericRecord convert(DeepNestedRecordEvent.Order event) {
        var genericRecord = new GenericData.Record(schema);
        genericRecord.put("orderId", event.orderId());
        genericRecord.put("shippingAddress", event.shippingAddress() != null ? deepNestedRecordEventOrderAddressToGenericRecordConverter.convert(event.shippingAddress()) : null);
        return genericRecord;
    }
}
