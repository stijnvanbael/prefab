package be.appify.prefab.example.streams;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;

@Event(topic = "${topics.streams.join-left}", platform = Event.Platform.KAFKA)
public record JoinLeftEvent(@PartitioningKey String id, String payload) {
}



