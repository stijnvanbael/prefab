package event.avro.infrastructure.avro;

import java.util.List;
import org.apache.avro.Schema;
import org.springframework.stereotype.Component;

@Component
public class DocFieldEventSchemaFactory {
    private final Schema schema;

    public DocFieldEventSchemaFactory() {
        this.schema = Schema.createRecord("DocFieldEvent", null, "event.avro", false, List.of(
                        new Schema.Field("name", Schema.create(Schema.Type.STRING), "The full name of the person", null),
                        new Schema.Field("age", Schema.create(Schema.Type.INT))
                    ));
    }

    public Schema createSchema() {
        return this.schema;
    }
}
