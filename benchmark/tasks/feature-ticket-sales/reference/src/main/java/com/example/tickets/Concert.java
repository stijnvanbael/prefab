package com.example.tickets;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.Computed;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Security;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById(security = @Security(enabled = false))
@GetList(security = @Security(enabled = false))
public record Concert(
        @Id Reference<Concert> id,
        @Version long version,
        @NotBlank String name,
        @NotBlank String venue,
        @NotBlank @Filter String city,
        @Positive int capacity,
        int ticketsSold
) {
    @Create(security = @Security(role = "ORGANIZER"))
    public Concert(@NotBlank String name, @NotBlank String venue, @NotBlank String city, @Positive int capacity) {
        this(Reference.create(), 0L, name, venue, city, capacity, 0);
    }

    @Computed
    public int remaining() {
        return Math.max(0, capacity - ticketsSold);
    }

    @Computed
    public boolean soldOut() {
        return remaining() == 0;
    }

    @EventHandler
    @ByReference(property = "concert")
    public Concert onTicketsOrdered(TicketOrderEvent.Placed event) {
        return new Concert(id, version, name, venue, city, capacity, ticketsSold + event.quantity());
    }

    @EventHandler
    @ByReference(property = "concert")
    public Concert onTicketsCancelled(TicketOrderEvent.Cancelled event) {
        return new Concert(id, version, name, venue, city, capacity, ticketsSold - event.quantity());
    }
}
