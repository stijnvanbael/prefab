package event.avro.infrastructure.avro;

import be.appify.prefab.core.avro.SchemaSupport;
import java.util.List;
import org.apache.avro.Schema;
import org.springframework.stereotype.Component;

@Component
public class NullableEventSchemaFactory {
    private final Schema schema;

    public NullableEventSchemaFactory() {
        this.schema = Schema.createRecord("NullableEvent", null, "event.avro", false, List.of(
                        new Schema.Field("id", Schema.create(Schema.Type.STRING)),
                        new Schema.Field("name", Schema.create(Schema.Type.STRING)),
                        new Schema.Field("description", SchemaSupport.createNullableSchema(Schema.create(Schema.Type.STRING)))
                    ));
    }

    public Schema createSchema() {
        return this.schema;
    }
}
