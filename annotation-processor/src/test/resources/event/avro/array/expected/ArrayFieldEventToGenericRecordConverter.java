package event.avro.infrastructure.avro;

import event.avro.ArrayFieldEvent;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ArrayFieldEventToGenericRecordConverter implements Converter<ArrayFieldEvent, GenericRecord> {
    private final Schema schema;

    private final ArrayFieldEventSaleLineToGenericRecordConverter arrayFieldEventSaleLineToGenericRecordConverter;

    public ArrayFieldEventToGenericRecordConverter(ArrayFieldEventSchemaFactory schemaFactory,
            ArrayFieldEventSaleLineToGenericRecordConverter arrayFieldEventSaleLineToGenericRecordConverter) {
        this.schema = schemaFactory.createSchema();
        this.arrayFieldEventSaleLineToGenericRecordConverter = arrayFieldEventSaleLineToGenericRecordConverter;
    }

    @Override
    public GenericRecord convert(ArrayFieldEvent event) {
        var genericRecord = new GenericData.Record(schema);
        genericRecord.put("tags", event.tags() != null ? new GenericData.Array(
                    schema.getField("tags").schema(),
                    event.tags().stream()
                        .map(item -> item)
                        .toList()
                ) : null);
        genericRecord.put("lines", event.lines() != null ? new GenericData.Array(
                    schema.getField("lines").schema(),
                    event.lines().stream()
                        .map(item -> item != null ? arrayFieldEventSaleLineToGenericRecordConverter.convert(item) : null)
                        .toList()
                ) : null);
        return genericRecord;
    }
}
