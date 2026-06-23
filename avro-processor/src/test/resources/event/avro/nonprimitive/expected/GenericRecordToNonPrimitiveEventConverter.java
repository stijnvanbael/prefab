package event.avro.infrastructure.avro;

import be.appify.prefab.avro.SchemaSupport;
import be.appify.prefab.core.service.Reference;
import event.avro.NonPrimitiveEvent;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component("event_avro_GenericRecordToNonPrimitiveEventConverter")
public class GenericRecordToNonPrimitiveEventConverter implements Converter<GenericRecord, NonPrimitiveEvent> {
    public GenericRecordToNonPrimitiveEventConverter() {
    }

    @Override
    public NonPrimitiveEvent convert(GenericRecord genericRecord) {
        return new NonPrimitiveEvent(
                    SchemaSupport.getField(genericRecord, "status") != null ? NonPrimitiveEvent.Status.valueOf(SchemaSupport.getField(genericRecord, "status").toString()) : null,
                    SchemaSupport.getField(genericRecord, "timestamp") != null ? Instant.ofEpochMilli((Long) SchemaSupport.getField(genericRecord, "timestamp")) : null,
                    SchemaSupport.getField(genericRecord, "date") != null ? LocalDate.ofEpochDay((Integer) SchemaSupport.getField(genericRecord, "date")) : null,
                    SchemaSupport.getField(genericRecord, "duration") != null ? Duration.ofMillis((Long) SchemaSupport.getField(genericRecord, "duration")) : null,
                    SchemaSupport.getField(genericRecord, "reference") != null ? SchemaSupport.getField(genericRecord, "reference") instanceof GenericRecord singleValueRecord
                        ? new Reference<Object>(SchemaSupport.getField(singleValueRecord, "id").toString())
                        : new Reference<Object>(SchemaSupport.getField(genericRecord, "reference").toString()) : null
                );
    }
}
