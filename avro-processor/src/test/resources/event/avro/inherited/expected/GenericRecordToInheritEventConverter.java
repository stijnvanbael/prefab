package event.avro.infrastructure.avro;

import be.appify.prefab.avro.SchemaSupport;
import event.avro.InheritEvent;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component("event_avro_GenericRecordToInheritEventConverter")
public class GenericRecordToInheritEventConverter implements Converter<GenericRecord, InheritEvent> {
    public GenericRecordToInheritEventConverter() {
    }

    @Override
    public InheritEvent convert(GenericRecord genericRecord) {
        return new InheritEvent(
                    SchemaSupport.getField(genericRecord, "superField").toString(),
                    SchemaSupport.getField(genericRecord, "subField").toString()
                );
    }
}
