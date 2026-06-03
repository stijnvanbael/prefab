package mother.nullablerecord.source;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record Order(
        @Id String id,
        @Version long version,
        ShipmentEvent.Address shippingAddress,
        List<ShipmentEvent.Item> items) {

    @Create
    public Order(
            @Nullable ShipmentEvent.Address shippingAddress,
            List<ShipmentEvent.Item> items,
            @Nullable List<ShipmentEvent.Item> optionalItems) {
        this(UUID.randomUUID().toString(), 0L, shippingAddress, items);
    }
}

