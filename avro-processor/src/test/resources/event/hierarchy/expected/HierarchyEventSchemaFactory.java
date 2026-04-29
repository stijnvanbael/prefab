package event.avro.infrastructure.avro;

import java.util.List;
import org.apache.avro.Schema;
import org.springframework.stereotype.Component;

@Component
public class HierarchyEventSchemaFactory {
    private final Schema schema;

    public HierarchyEventSchemaFactory(
            HierarchyEventCreatedSchemaFactory hierarchyEventCreatedSchemaFactory,
            HierarchyEventUpdatedSchemaFactory hierarchyEventUpdatedSchemaFactory) {
        this.schema = Schema.createUnion(List.of(
                    hierarchyEventCreatedSchemaFactory.createSchema(),
                    hierarchyEventUpdatedSchemaFactory.createSchema()
                ));
    }

    public Schema createSchema() {
        return this.schema;
    }
}
