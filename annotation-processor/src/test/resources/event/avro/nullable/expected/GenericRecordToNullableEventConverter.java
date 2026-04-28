package event.avro.infrastructure.avro;

import event.avro.NullableEvent;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class GenericRecordToNullableEventConverter implements Converter<GenericRecord, NullableEvent> {
    public GenericRecordToNullableEventConverter() {
    }

    @Override
    public NullableEvent convert(GenericRecord genericRecord) {
        return new NullableEvent(
                    genericRecord.get("id").toString(),
                    genericRecord.get("name").toString(),
                    genericRecord.get("description") != null ? genericRecord.get("description").toString() : null
                );
    }
}
