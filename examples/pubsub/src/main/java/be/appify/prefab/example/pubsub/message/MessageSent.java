package be.appify.prefab.example.pubsub.message;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.pubsub.channel.Channel;

@Event(topic = "${topics.message.name}")
public record MessageSent(@PartitioningKey String id, Reference<Channel> channel) {
}
