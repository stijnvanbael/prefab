package be.appify.prefab.example.pubsub.channel;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;

@Event(topic = "${topics.channel.name}")
public record ChannelCreated(
        @PartitioningKey String id,
        String name
) {
}
