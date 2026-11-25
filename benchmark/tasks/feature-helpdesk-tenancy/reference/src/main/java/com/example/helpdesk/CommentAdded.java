package com.example.helpdesk;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;

@Event(topic = "ticket-comments")
public record CommentAdded(@PartitioningKey Reference<Ticket> ticket, Reference<Comment> comment) {
}
