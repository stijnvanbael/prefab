package event.avro.infrastructure.avro;

import event.avro.InheritEvent;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class GenericRecordToInheritEventConverter implements Converter<GenericRecord, InheritEvent> {
    public GenericRecordToInheritEventConverter() {
    }

    @Override
    public InheritEvent convert(GenericRecord genericRecord) {
        return new InheritEvent(
                    genericRecord.get("superField").toString(),
                    genericRecord.get("subField").toString()
                );
    }
}
