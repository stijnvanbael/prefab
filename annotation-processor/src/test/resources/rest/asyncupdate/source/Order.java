package rest.asyncupdate;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.AsyncCommit;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.PublishesEvents;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;

@Aggregate
@AsyncCommit
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record Order(
        @Id Reference<Order> id,
        String trackingCode,
        String status
) implements PublishesEvents {
    @Update(path = "/ship", method = "POST")
    public void ship(@NotNull String trackingCode) {
        publish(new OrderShipped(id, trackingCode));
    }

    @Event(topic = "orders")
    public record OrderShipped(Reference<Order> id, String trackingCode) {
    }
}

