package event.avro.infrastructure.avro;

import java.util.List;
import org.apache.avro.Schema;
import org.springframework.stereotype.Component;

@Component("event_avro_DeepNestedRecordEventSchemaFactory")
public class DeepNestedRecordEventSchemaFactory {
    private final Schema schema;

    public DeepNestedRecordEventSchemaFactory(
            DeepNestedRecordEventOrderSchemaFactory deepNestedRecordEventOrderSchemaFactory) {
        this.schema = Schema.createRecord("DeepNestedRecordEvent", null, "event.avro", false, List.of(
                        new Schema.Field("id", Schema.create(Schema.Type.STRING)),
                        new Schema.Field("order", deepNestedRecordEventOrderSchemaFactory.createSchema())
                    ));
    }

    public Schema createSchema() {
        return this.schema;
    }
}
