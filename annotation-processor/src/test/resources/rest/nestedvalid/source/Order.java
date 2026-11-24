package rest.nestedvalid;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record Order(
        @Id String id,
        @Version long version,
        String reference,
        Address shippingAddress) {

    @Create
    public Order(String reference, @Valid Address shippingAddress) {
        this(UUID.randomUUID().toString(), 0L, reference, shippingAddress);
    }

    @Update
    public Order ship(@Valid Address shippingAddress) {
        return new Order(id, version, reference, shippingAddress);
    }
}
