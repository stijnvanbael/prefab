package event.handler.optionalhandleraudit;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.audit.AuditInfo;
import be.appify.prefab.core.service.Reference;
import java.util.Optional;

import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record Order(
        @Id Reference<Order> id,
        @Version long version,
        String title,
        AuditInfo audit
) {
    @EventHandler
    public static Optional<Order> onCreate(OrderCreated event) {
        return Optional.of(new Order(Reference.create(), 0L, event.orderId(), new AuditInfo()));
    }
}

