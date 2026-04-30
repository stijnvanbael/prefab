package be.appify.prefab.example.pubsub.channel;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.pubsub.message.MessageSent;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ChannelCreated.class, name = "ChannelCreated"),
        @JsonSubTypes.Type(value = MessageSent.class, name = "MessageSent")
})
@Event(topic = "${topics.channel.name}")
public interface ChannelEvent {
    @PartitioningKey
    Reference<Channel> channel();
}
