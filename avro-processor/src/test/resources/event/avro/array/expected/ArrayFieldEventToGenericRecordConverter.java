package event.avro.infrastructure.avro;

import be.appify.prefab.avro.SchemaSupport;
import event.avro.ArrayFieldEvent;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component("event_avro_ArrayFieldEventToGenericRecordConverter")
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
                    SchemaSupport.arraySchemaOf(schema.getField("tags").schema()),
                    event.tags().stream()
                        .map(item -> item)
                        .toList()
                ) : null);
        genericRecord.put("lines", event.lines() != null ? new GenericData.Array(
                    SchemaSupport.arraySchemaOf(schema.getField("lines").schema()),
                    event.lines().stream()
                        .map(item -> item != null ? arrayFieldEventSaleLineToGenericRecordConverter.convert(item) : null)
                        .toList()
                ) : null);
        return genericRecord;
    }
}
