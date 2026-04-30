package event.avro.infrastructure.avro;

import java.util.List;
import org.apache.avro.Schema;
import org.springframework.stereotype.Component;

@Component
public class SingleValuedNestedEventOuterSchemaFactory {
    private final Schema schema;

    public SingleValuedNestedEventOuterSchemaFactory() {
        this.schema = Schema.createRecord("SingleValuedNestedEvent_Outer", null, "event.avro", false, List.of(
                        new Schema.Field("inner", Schema.create(Schema.Type.STRING))
                    ));
    }

    public Schema createSchema() {
        return this.schema;
    }
}
