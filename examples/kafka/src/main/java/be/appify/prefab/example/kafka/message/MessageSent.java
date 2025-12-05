package be.appify.prefab.example.kafka.message;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.kafka.channel.Channel;

@Event(topic = "${topics.message.name}", platform = Event.Platform.KAFKA, publishedBy = Channel.class)
public record MessageSent(@PartitioningKey String id, Reference<Channel> channel) {
}
