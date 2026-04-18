package rest.aggregate.renamed;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.service.Reference;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
public record Shipment(
        @Id String id,
        @Version long version,
        String note,
        Reference<rest.aggregate.renamed.Item> assignedItem) {

    @Create
    public Shipment(String note, rest.aggregate.renamed.Item cargo) {
        this(UUID.randomUUID().toString(), 0L, note, new Reference<>(cargo.id()));
    }

    @Update
    public Shipment reassign(rest.aggregate.renamed.Item cargo) {
        return new Shipment(id, version, note, new Reference<>(cargo.id()));
    }
}

