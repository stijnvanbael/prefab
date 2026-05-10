package event.avro.infrastructure.avro;

import event.avro.SuperType;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component("event_avro_GenericRecordToSuperTypeConverter")
public class GenericRecordToSuperTypeConverter implements Converter<GenericRecord, SuperType> {
    private final GenericRecordToInheritEventConverter genericRecordToInheritEventConverter;

    public GenericRecordToSuperTypeConverter(GenericRecordToInheritEventConverter genericRecordToInheritEventConverter) {
        this.genericRecordToInheritEventConverter = genericRecordToInheritEventConverter;
    }

    @Override
    public SuperType convert(GenericRecord genericRecord) {
        return switch (genericRecord.getSchema().getName()) {
            case "InheritEvent" -> genericRecordToInheritEventConverter.convert(genericRecord);
            default -> throw new IllegalArgumentException("Unknown schema: " + genericRecord.getSchema().getName());
        };
    }
}

