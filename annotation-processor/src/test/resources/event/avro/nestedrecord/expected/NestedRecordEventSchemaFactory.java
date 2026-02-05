package event.avro.infrastructure.avro;

import java.util.List;
import org.apache.avro.Schema;
import org.springframework.stereotype.Component;

@Component
public class NestedRecordEventSchemaFactory {
    private final Schema schema;

    public NestedRecordEventSchemaFactory(
            NestedRecordEventMoneySchemaFactory nestedRecordEventMoneySchemaFactory) {
        this.schema = Schema.createRecord("NestedRecordEvent", null, "event.avro", false, List.of(
                        new Schema.Field("id", Schema.create(Schema.Type.STRING)),
                        new Schema.Field("totalAmount", nestedRecordEventMoneySchemaFactory.createSchema()),
                        new Schema.Field("paidAmount", nestedRecordEventMoneySchemaFactory.createSchema())
                    ));
    }

    public Schema createSchema() {
        return this.schema;
    }
}
