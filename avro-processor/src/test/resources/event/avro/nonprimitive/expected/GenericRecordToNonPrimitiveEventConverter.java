package event.avro.infrastructure.avro;

import be.appify.prefab.avro.SchemaSupport;
import be.appify.prefab.core.service.Reference;
import event.avro.NonPrimitiveEvent;
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
                    SchemaSupport.getEnum(genericRecord, "status", NonPrimitiveEvent.Status.class),
                    SchemaSupport.getInstant(genericRecord, "timestamp"),
                    SchemaSupport.getLocalDate(genericRecord, "date"),
                    SchemaSupport.getDuration(genericRecord, "duration"),
                    SchemaSupport.getField(genericRecord, "reference") != null ? SchemaSupport.getField(genericRecord, "reference") instanceof GenericRecord singleValueRecord
                        ? new Reference<Object>(SchemaSupport.getField(singleValueRecord, "id").toString())
                        : new Reference<Object>(SchemaSupport.getField(genericRecord, "reference").toString()) : null
                );
    }
}
