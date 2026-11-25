package com.example.helpdesk;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.TenantId;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Parent;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
@GetList
public record Comment(
        @Id Reference<Comment> id,
        @Version long version,
        @TenantId String tenant,
        @Parent @NotNull Reference<Ticket> ticket,
        @NotBlank String author,
        @NotBlank String text
) {
    private static final EventPublisher<CommentAdded> ADDED = EventPublisher.bind(CommentAdded.class);

    @Create
    public Comment(@NotNull Reference<Ticket> ticket, @NotBlank String author, @NotBlank String text) {
        this(Reference.create(), 0L, null, ticket, author, text);
        ADDED.publish(new CommentAdded(ticket, id));
    }
}
