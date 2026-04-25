package be.appify.prefab.example.pubsub.message;

import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.pubsub.channel.Channel;
import be.appify.prefab.example.pubsub.channel.ChannelEvent;

public record MessageSent(Reference<Message> id, @PartitioningKey Reference<Channel> channel) implements ChannelEvent {
}
