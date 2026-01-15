package be.appify.prefab.example.kafka.user;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.kafka.channel.Channel;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@Event(topic = "${topics.user.name}")
public sealed interface UserEvent permits UserEvent.Created, UserEvent.SubscribedToChannel {
    @PartitioningKey
    String id();

    record Created(String id, String name) implements UserEvent {
    }

    record SubscribedToChannel(String id, Reference<Channel> channel) implements UserEvent {
    }
}

