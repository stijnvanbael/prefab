package be.appify.prefab.example.kafka.channel;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;

@Event(topic = "${topics.channel.name}")
public record ChannelCreated(
        @PartitioningKey String id,
        String name
) {
}
