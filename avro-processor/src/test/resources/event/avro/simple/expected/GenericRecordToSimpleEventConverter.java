package event.avro.infrastructure.avro;

import event.avro.SimpleEvent;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class GenericRecordToSimpleEventConverter implements Converter<GenericRecord, SimpleEvent> {
    public GenericRecordToSimpleEventConverter() {
    }

    @Override
    public SimpleEvent convert(GenericRecord genericRecord) {
        return new SimpleEvent(
                    genericRecord.get("name").toString(),
                    (Integer) genericRecord.get("age"),
                    (Double) genericRecord.get("score"),
                    (Boolean) genericRecord.get("active")
                );
    }
}
