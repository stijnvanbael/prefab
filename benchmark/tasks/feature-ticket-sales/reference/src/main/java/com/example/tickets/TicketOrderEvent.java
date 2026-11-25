package com.example.tickets;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@Event(topic = "ticket-orders")
public sealed interface TicketOrderEvent permits TicketOrderEvent.Placed, TicketOrderEvent.Cancelled {
    @PartitioningKey
    Reference<Concert> concert();

    int quantity();

    record Placed(Reference<TicketOrder> order, Reference<Concert> concert, int quantity) implements TicketOrderEvent {
    }

    record Cancelled(Reference<TicketOrder> order, Reference<Concert> concert, int quantity)
            implements TicketOrderEvent {
    }
}
