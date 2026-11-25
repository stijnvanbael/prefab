package com.example.helpdesk;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.TenantId;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
@GetList
public record Ticket(
        @Id Reference<Ticket> id,
        @Version long version,
        @TenantId String tenant,
        @NotBlank String subject,
        String description,
        @Filter TicketStatus status,
        int commentCount
) {
    @Create
    public Ticket(@NotBlank String subject, String description) {
        this(Reference.create(), 0L, null, subject, description, TicketStatus.OPEN, 0);
    }

    @Update(path = "/resolution", method = "POST")
    public Ticket resolve() {
        return new Ticket(id, version, tenant, subject, description, TicketStatus.RESOLVED, commentCount);
    }

    @EventHandler
    @ByReference(property = "ticket")
    public Ticket onCommentAdded(CommentAdded event) {
        var next = status == TicketStatus.OPEN ? TicketStatus.IN_PROGRESS : status;
        return new Ticket(id, version, tenant, subject, description, next, commentCount + 1);
    }
}
