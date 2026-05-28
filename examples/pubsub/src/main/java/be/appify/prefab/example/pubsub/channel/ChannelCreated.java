package be.appify.prefab.example.pubsub.channel;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.service.Reference;

@Event(topic = "${topics.channel.name}")
public record ChannelCreated(
        Reference<Channel> channel,
        String name
) {
}
