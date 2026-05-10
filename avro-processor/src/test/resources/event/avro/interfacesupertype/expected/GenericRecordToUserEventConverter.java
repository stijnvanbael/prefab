package event.avro.infrastructure.avro;

import event.avro.UserEvent;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component("event_avro_GenericRecordToUserEventConverter")
public class GenericRecordToUserEventConverter implements Converter<GenericRecord, UserEvent> {
    private final GenericRecordToUserCreatedConverter genericRecordToUserCreatedConverter;

    public GenericRecordToUserEventConverter(GenericRecordToUserCreatedConverter genericRecordToUserCreatedConverter) {
        this.genericRecordToUserCreatedConverter = genericRecordToUserCreatedConverter;
    }

    @Override
    public UserEvent convert(GenericRecord genericRecord) {
        return switch (genericRecord.getSchema().getName()) {
            case "UserCreated" -> genericRecordToUserCreatedConverter.convert(genericRecord);
            default -> throw new IllegalArgumentException("Unknown schema: " + genericRecord.getSchema().getName());
        };
    }
}

