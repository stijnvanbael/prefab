package event.avro.infrastructure.avro;

import be.appify.prefab.core.service.Reference;
import event.avro.NonPrimitiveEvent;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class GenericRecordToNonPrimitiveEventConverter implements Converter<GenericRecord, NonPrimitiveEvent> {
    public GenericRecordToNonPrimitiveEventConverter() {
    }

    @Override
    public NonPrimitiveEvent convert(GenericRecord genericRecord) {
        return new NonPrimitiveEvent(
                    genericRecord.get("status") != null ? NonPrimitiveEvent.Status.valueOf(genericRecord.get("status").toString()) : null,
                    genericRecord.get("timestamp") != null ? Instant.ofEpochMilli((Long) genericRecord.get("timestamp")) : null,
                    genericRecord.get("date") != null ? LocalDate.ofEpochDay((Integer) genericRecord.get("date")) : null,
                    genericRecord.get("duration") != null ? Duration.ofMillis((Long) genericRecord.get("duration")) : null,
                    genericRecord.get("reference") != null ? Reference.fromId(genericRecord.get("reference").toString()) : null
                );
    }
}
