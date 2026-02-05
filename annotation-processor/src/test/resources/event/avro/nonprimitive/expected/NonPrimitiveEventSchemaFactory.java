package event.avro.infrastructure.avro;

import be.appify.prefab.core.avro.SchemaSupport;
import java.util.List;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.springframework.stereotype.Component;

@Component
public class NonPrimitiveEventSchemaFactory {
    private final Schema schema;

    public NonPrimitiveEventSchemaFactory() {
        this.schema = Schema.createRecord("NonPrimitiveEvent", null, "event.avro", false, List.of(
                        new Schema.Field("status", Schema.createEnum("NonPrimitiveEvent_Status", null, "event.avro", List.of("ACTIVE", "INACTIVE", "PENDING"))),
                        new Schema.Field("timestamp", SchemaSupport.createLogicalSchema(Schema.Type.LONG, LogicalTypes.timestampMillis())),
                        new Schema.Field("date", SchemaSupport.createLogicalSchema(Schema.Type.INT, LogicalTypes.date())),
                        new Schema.Field("duration", SchemaSupport.createLogicalSchema(Schema.Type.LONG, SchemaSupport.DURATION_MILLIS)),
                        new Schema.Field("reference", SchemaSupport.createLogicalSchema(Schema.Type.STRING, SchemaSupport.REFERENCE))
                    ));
    }

    public Schema createSchema() {
        return this.schema;
    }
}
