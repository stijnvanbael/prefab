package event.avro.infrastructure.avro;

import be.appify.prefab.core.avro.SchemaSupport;
import java.util.List;
import org.apache.avro.Schema;
import org.springframework.stereotype.Component;

@Component
public class ExampleFieldEventSchemaFactory {
    private final Schema schema;

    public ExampleFieldEventSchemaFactory() {
        this.schema = Schema.createRecord("ExampleFieldEvent", null, "event.avro", false, List.of(
                        SchemaSupport.withSample(new Schema.Field("name", Schema.create(Schema.Type.STRING)), "john-doe"),
                        new Schema.Field("age", Schema.create(Schema.Type.INT))
                    ));
    }

    public Schema createSchema() {
        return this.schema;
    }
}
