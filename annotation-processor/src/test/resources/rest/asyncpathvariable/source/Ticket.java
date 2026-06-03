package rest.asyncpathvariable;

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
@AsyncCommit
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record Ticket(
        @Id Reference<Ticket> id,
        String queue,
        String subject,
        String status
) implements PublishesEvents {

    @Create(path = "/{queue}")
    public static void open(String queue, @NotNull String subject) {
        PublishesEvents.publishEvent(new TicketOpened(Reference.create(), queue, subject));
    }

    @Update(path = "/{status}/transition")
    public void transition(String status) {
        publish(new TicketTransitioned(id, status));
    }

    @Event(topic = "tickets")
    public record TicketOpened(Reference<Ticket> id, String queue, String subject) {
    }

    @Event(topic = "tickets")
    public record TicketTransitioned(Reference<Ticket> id, String status) {
    }
}

