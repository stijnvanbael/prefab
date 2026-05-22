package event.avro.infrastructure.avro;

import event.avro.OverriddenUserEvent;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component("event_avro_GenericRecordToOverriddenUserEventConverter")
public class GenericRecordToOverriddenUserEventConverter implements Converter<GenericRecord, OverriddenUserEvent> {
    private final GenericRecordToUserCreatedWithCustomSchemaConverter genericRecordToUserCreatedWithCustomSchemaConverter;

    public GenericRecordToOverriddenUserEventConverter(GenericRecordToUserCreatedWithCustomSchemaConverter genericRecordToUserCreatedWithCustomSchemaConverter) {
        this.genericRecordToUserCreatedWithCustomSchemaConverter = genericRecordToUserCreatedWithCustomSchemaConverter;
    }

    @Override
    public OverriddenUserEvent convert(GenericRecord genericRecord) {
        return switch (genericRecord.getSchema().getName()) {
            case "UserCreatedV1" -> genericRecordToUserCreatedWithCustomSchemaConverter.convert(genericRecord);
            default -> throw new IllegalArgumentException("Unknown schema: " + genericRecord.getSchema().getName());
        };
    }
}

