package be.appify.prefab.example.streams;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;

@Event(topic = "${topics.streams.join-right}", platform = Event.Platform.KAFKA)
public record JoinRightEvent(@PartitioningKey String id, String tag) {
}



