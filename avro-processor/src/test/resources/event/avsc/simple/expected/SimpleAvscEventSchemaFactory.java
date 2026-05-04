package event.avsc.infrastructure.avro;

import java.util.List;
import org.apache.avro.Schema;
import org.springframework.stereotype.Component;

@Component("event_avsc_SimpleAvscEventSchemaFactory")
public class SimpleAvscEventSchemaFactory {
    private final Schema schema;

    public SimpleAvscEventSchemaFactory() {
        this.schema = Schema.createRecord("SimpleAvscEvent", null, "event.avsc", false, List.of(
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
