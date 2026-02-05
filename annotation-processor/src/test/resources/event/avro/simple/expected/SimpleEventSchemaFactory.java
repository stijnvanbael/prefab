package event.avro.infrastructure.avro;

import java.util.List;
import org.apache.avro.Schema;
import org.springframework.stereotype.Component;

@Component
public class SimpleEventSchemaFactory {
    private final Schema schema;

    public SimpleEventSchemaFactory() {
        this.schema = Schema.createRecord("SimpleEvent", null, "event.avro", false, List.of(
                        new Schema.Field("name", Schema.create(Schema.Type.STRING)),
                        new Schema.Field("age", Schema.create(Schema.Type.INT)),
                        new Schema.Field("score", Schema.create(Schema.Type.DOUBLE)),
                        new Schema.Field("active", Schema.create(Schema.Type.BOOLEAN))
                    ));
    }

    public Schema createSchema() {
        return this.schema;
    }
}
