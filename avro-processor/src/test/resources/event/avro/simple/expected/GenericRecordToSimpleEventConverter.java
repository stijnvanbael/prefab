package event.avro.infrastructure.avro;

import be.appify.prefab.avro.SchemaSupport;
import event.avro.SimpleEvent;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component("event_avro_GenericRecordToSimpleEventConverter")
public class GenericRecordToSimpleEventConverter implements Converter<GenericRecord, SimpleEvent> {
    public GenericRecordToSimpleEventConverter() {
    }

    @Override
    public SimpleEvent convert(GenericRecord genericRecord) {
        return new SimpleEvent(
                    SchemaSupport.getField(genericRecord, "name").toString(),
                    (Integer) SchemaSupport.getField(genericRecord, "age"),
                    (Double) SchemaSupport.getField(genericRecord, "score"),
                    (Boolean) SchemaSupport.getField(genericRecord, "active")
                );
    }
}
