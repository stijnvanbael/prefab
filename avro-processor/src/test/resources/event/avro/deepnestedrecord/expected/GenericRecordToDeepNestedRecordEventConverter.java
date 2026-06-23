package event.avro.infrastructure.avro;

import be.appify.prefab.avro.SchemaSupport;
import event.avro.DeepNestedRecordEvent;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component("event_avro_GenericRecordToDeepNestedRecordEventConverter")
public class GenericRecordToDeepNestedRecordEventConverter implements Converter<GenericRecord, DeepNestedRecordEvent> {
    private final GenericRecordToDeepNestedRecordEventOrderConverter genericRecordToDeepNestedRecordEventOrderConverter;

    public GenericRecordToDeepNestedRecordEventConverter(
            GenericRecordToDeepNestedRecordEventOrderConverter genericRecordToDeepNestedRecordEventOrderConverter) {
        this.genericRecordToDeepNestedRecordEventOrderConverter = genericRecordToDeepNestedRecordEventOrderConverter;
    }

    @Override
    public DeepNestedRecordEvent convert(GenericRecord genericRecord) {
        return new DeepNestedRecordEvent(
                    SchemaSupport.getString(genericRecord, "id"),
                    SchemaSupport.getRecord(genericRecord, "order", genericRecordToDeepNestedRecordEventOrderConverter::convert)
                );
    }
}
