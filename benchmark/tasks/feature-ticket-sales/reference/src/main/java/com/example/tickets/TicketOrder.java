package com.example.tickets;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record TicketOrder(
        @Id Reference<TicketOrder> id,
        @Version long version,
        @NotNull Reference<Concert> concert,
        @Min(1) @Max(10) int quantity,
        @NotNull TicketOrderStatus status
) {
    private static final EventPublisher<TicketOrderEvent> EVENTS = EventPublisher.bind(TicketOrderEvent.class);

    @Create
    public TicketOrder(@NotNull Reference<Concert> concert, @Min(1) @Max(10) int quantity) {
        this(Reference.create(), 0L, concert, quantity, TicketOrderStatus.PLACED);
        EVENTS.publish(new TicketOrderEvent.Placed(id, concert, quantity));
    }

    @Update(path = "/cancellation", method = "POST")
    public TicketOrder cancel() {
        if (status == TicketOrderStatus.CANCELLED) {
            return this;
        }
        EVENTS.publish(new TicketOrderEvent.Cancelled(id, concert, quantity));
        return new TicketOrder(id, version, concert, quantity, TicketOrderStatus.CANCELLED);
    }
}
