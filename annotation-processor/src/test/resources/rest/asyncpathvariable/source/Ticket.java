package rest.asyncpathvariable;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.AsyncCommit;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.PublishesEvents;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;

@Aggregate
@AsyncCommit
public record Ticket(
        @Id Reference<Ticket> id,
        String queue,
        String subject,
        String status
) implements PublishesEvents {

    @Create(path = "/{queue}")
    public static TicketOpened open(String queue, @NotNull String subject) {
        return new TicketOpened(Reference.create(), queue, subject);
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

