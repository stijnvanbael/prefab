package rest.asyncmethodlevel;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.AsyncCommit;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.PublishesEvents;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;

@Aggregate
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record Order(
        @Id Reference<Order> id,
        String customerId,
        String status
) implements PublishesEvents {
    @Create
    @AsyncCommit
    public static void placeOrder(@NotNull String customerId) {
        PublishesEvents.publishEvent(new OrderPlaced(Reference.create(), customerId));
    }

    @Update(path = "/cancel")
    @AsyncCommit
    public void cancel() {
        publish(new OrderCancelled(id));
    }

    @Event(topic = "orders")
    public record OrderPlaced(Reference<Order> id, String customerId) {
    }

    @Event(topic = "orders")
    public record OrderCancelled(Reference<Order> id) {
    }
}

