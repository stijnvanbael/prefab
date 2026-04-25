package be.appify.prefab.example.pubsub.channel;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@Event(topic = "${topics.channel.name}")
public interface ChannelEvent {
    @PartitioningKey
    Reference<Channel> channel();
}
