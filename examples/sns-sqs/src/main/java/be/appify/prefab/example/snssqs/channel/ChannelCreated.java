package be.appify.prefab.example.snssqs.channel;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;

@Event(topic = "${topics.channel.name}")
public record ChannelCreated(
        @PartitioningKey Reference<Channel> reference,
        String name
) {
}
