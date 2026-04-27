package be.appify.prefab.example.pubsub.channel;

import be.appify.prefab.core.service.Reference;

public record ChannelCreated(
        Reference<Channel> channel,
        String name
) implements ChannelEvent {
}
