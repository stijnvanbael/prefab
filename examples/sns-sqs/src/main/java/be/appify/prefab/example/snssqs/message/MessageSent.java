package be.appify.prefab.example.snssqs.message;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.snssqs.channel.Channel;

@Event(topic = "${topics.message.name}")
public record MessageSent(@PartitioningKey Reference<Message> id, Reference<Channel> channel) {
}
