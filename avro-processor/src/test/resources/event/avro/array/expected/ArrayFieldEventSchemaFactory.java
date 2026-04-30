package event.avro.infrastructure.avro;

import java.util.List;
import org.apache.avro.Schema;
import org.springframework.stereotype.Component;

@Component
public class ArrayFieldEventSchemaFactory {
    private final Schema schema;

    public ArrayFieldEventSchemaFactory(
            ArrayFieldEventSaleLineSchemaFactory arrayFieldEventSaleLineSchemaFactory) {
        this.schema = Schema.createRecord("ArrayFieldEvent", null, "event.avro", false, List.of(
                        new Schema.Field("tags", Schema.createArray(Schema.create(Schema.Type.STRING))),
                        new Schema.Field("lines", Schema.createArray(arrayFieldEventSaleLineSchemaFactory.createSchema()))
                    ));
    }

    public Schema createSchema() {
        return this.schema;
    }
}
