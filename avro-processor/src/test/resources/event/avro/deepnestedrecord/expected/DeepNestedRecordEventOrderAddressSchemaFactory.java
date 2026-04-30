package event.avro.infrastructure.avro;

import java.util.List;
import org.apache.avro.Schema;
import org.springframework.stereotype.Component;

@Component("event_avro_DeepNestedRecordEventOrderAddressSchemaFactory")
public class DeepNestedRecordEventOrderAddressSchemaFactory {
    private final Schema schema;

    public DeepNestedRecordEventOrderAddressSchemaFactory() {
        this.schema = Schema.createRecord("DeepNestedRecordEvent_Order_Address", null, "event.avro", false, List.of(
                        new Schema.Field("street", Schema.create(Schema.Type.STRING)),
                        new Schema.Field("city", Schema.create(Schema.Type.STRING))
                    ));
    }

    public Schema createSchema() {
        return this.schema;
    }
}
