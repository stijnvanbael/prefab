package event.avro.infrastructure.avro;

import java.util.List;
import org.apache.avro.Schema;
import org.springframework.stereotype.Component;

@Component
public class InheritEventSchemaFactory {
    private final Schema schema;

    public InheritEventSchemaFactory() {
        this.schema = Schema.createRecord("InheritEvent", null, "event.avro", false, List.of(
                        new Schema.Field("superField", Schema.create(Schema.Type.STRING)),
                        new Schema.Field("subField", Schema.create(Schema.Type.STRING))
                    ));
    }

    public Schema createSchema() {
        return this.schema;
    }
}
