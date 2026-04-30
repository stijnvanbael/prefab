package event.avro.infrastructure.avro;

import java.util.List;
import org.apache.avro.Schema;
import org.springframework.stereotype.Component;

@Component("event_avro_SingleValuedNestedEventSchemaFactory")
public class SingleValuedNestedEventSchemaFactory {
    private final Schema schema;

    public SingleValuedNestedEventSchemaFactory(
            SingleValuedNestedEventOuterSchemaFactory singleValuedNestedEventOuterSchemaFactory) {
        this.schema = Schema.createRecord("SingleValuedNestedEvent", null, "event.avro", false, List.of(
                        new Schema.Field("id", Schema.create(Schema.Type.STRING)),
                        new Schema.Field("outer", singleValuedNestedEventOuterSchemaFactory.createSchema())
                    ));
    }

    public Schema createSchema() {
        return this.schema;
    }
}
