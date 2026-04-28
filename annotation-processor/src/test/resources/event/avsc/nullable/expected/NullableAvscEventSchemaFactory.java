package event.avsc.infrastructure.avro;

import be.appify.prefab.core.avro.SchemaSupport;
import java.util.List;
import org.apache.avro.Schema;
import org.springframework.stereotype.Component;

@Component
public class NullableAvscEventSchemaFactory {
    private final Schema schema;

    public NullableAvscEventSchemaFactory() {
        this.schema = Schema.createRecord("NullableAvscEvent", null, "event.avsc", false, List.of(
                        new Schema.Field("id", Schema.create(Schema.Type.STRING)),
                        new Schema.Field("name", Schema.create(Schema.Type.STRING)),
                        new Schema.Field("description", SchemaSupport.createNullableSchema(Schema.create(Schema.Type.STRING)), null, Schema.Field.NULL_DEFAULT_VALUE)
                    ));
    }

    public Schema createSchema() {
        return this.schema;
    }
}
