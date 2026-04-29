package event.avro.infrastructure.avro;

import event.avro.DeepNestedRecordEvent;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class GenericRecordToDeepNestedRecordEventConverter implements Converter<GenericRecord, DeepNestedRecordEvent> {
    private final GenericRecordToDeepNestedRecordEventOrderConverter genericRecordToDeepNestedRecordEventOrderConverter;

    public GenericRecordToDeepNestedRecordEventConverter(
            GenericRecordToDeepNestedRecordEventOrderConverter genericRecordToDeepNestedRecordEventOrderConverter) {
        this.genericRecordToDeepNestedRecordEventOrderConverter = genericRecordToDeepNestedRecordEventOrderConverter;
    }

    @Override
    public DeepNestedRecordEvent convert(GenericRecord genericRecord) {
        return new DeepNestedRecordEvent(
                    genericRecord.get("id").toString(),
                    genericRecord.get("order") != null ? genericRecordToDeepNestedRecordEventOrderConverter.convert((GenericRecord) genericRecord.get("order")) : null
                );
    }
}
