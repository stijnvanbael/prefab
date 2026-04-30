package event.avro.infrastructure.avro;

import java.util.List;
import org.apache.avro.Schema;
import org.springframework.stereotype.Component;

@Component("event_avro_DeepNestedRecordEventOrderSchemaFactory")
public class DeepNestedRecordEventOrderSchemaFactory {
    private final Schema schema;

    public DeepNestedRecordEventOrderSchemaFactory(
            DeepNestedRecordEventOrderAddressSchemaFactory deepNestedRecordEventOrderAddressSchemaFactory) {
        this.schema = Schema.createRecord("DeepNestedRecordEvent_Order", null, "event.avro", false, List.of(
                        new Schema.Field("orderId", Schema.create(Schema.Type.STRING)),
                        new Schema.Field("shippingAddress", deepNestedRecordEventOrderAddressSchemaFactory.createSchema())
                    ));
    }

    public Schema createSchema() {
        return this.schema;
    }
}
