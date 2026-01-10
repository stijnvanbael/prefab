package be.appify.prefab.example.kafka.user;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.PartitioningKey;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.kafka.channel.Channel;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
// Until Java 17 support for @JsonTypeInfo with sealed interfaces is added in Jackson 3.0, we need to explicitly list the subtypes
@JsonSubTypes({
        @JsonSubTypes.Type(UserEvent.Created.class),
        @JsonSubTypes.Type(UserEvent.SubscribedToChannel.class)
})
@Event(topic = "${topics.user.name}")
public sealed interface UserEvent permits UserEvent.Created, UserEvent.SubscribedToChannel {
    @PartitioningKey
    String id();

    record Created(String id, String name) implements UserEvent {
    }

    record SubscribedToChannel(String id, Reference<Channel> channel) implements UserEvent {
    }
}

