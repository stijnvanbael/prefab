package be.appify.prefab.example.snssqs.user;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.snssqs.channel.Channel;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@Event(topic = "${topics.user.name}")
public sealed interface UserEvent permits UserEvent.Created, UserEvent.SubscribedToChannel {
    @PartitioningKey
    Reference<User> reference();

    record Created(Reference<User> reference, String name) implements UserEvent {
    }

    record SubscribedToChannel(Reference<User> reference, Reference<Channel> channel) implements UserEvent {
    }
}

