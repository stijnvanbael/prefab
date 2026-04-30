package event.avro.infrastructure.avro;

import java.util.List;
import org.apache.avro.Schema;
import org.springframework.stereotype.Component;

@Component
public class NestedRecordEventMoneySchemaFactory {
    private final Schema schema;

    public NestedRecordEventMoneySchemaFactory() {
        this.schema = Schema.createRecord("NestedRecordEvent_Money", null, "event.avro", false, List.of(
                        new Schema.Field("currency", Schema.create(Schema.Type.STRING)),
                        new Schema.Field("amount", Schema.create(Schema.Type.DOUBLE))
                    ));
    }

    public Schema createSchema() {
        return this.schema;
    }
}
