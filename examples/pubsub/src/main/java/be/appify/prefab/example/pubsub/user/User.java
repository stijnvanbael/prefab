package be.appify.prefab.example.pubsub.user;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.PublishesEvents;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.pubsub.channel.Channel;
import be.appify.prefab.example.pubsub.message.Message;
import be.appify.prefab.example.pubsub.message.MessageSent;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Aggregate
@DbMigration
@GetById
public record User(
        @Id String id,
        @Version long version,
        @NotNull String name,
        @NotNull List<String> channelSubscriptions, // Currently, Spring Data JDBC cannot support List<Reference<T>>
        @NotNull List<UnreadMessage> unreadMessages
) implements PublishesEvents {
    @PersistenceCreator
    public User {
    }

    @Create
    public User(@NotNull String name) {
        this(UUID.randomUUID().toString(), 0L, name, new ArrayList<>(), new ArrayList<>());
        publish(new UserEvent.Created(id, name));
    }

    @Update(path = "/channel-subscriptions", method = "POST")
    public void subscribeToChannel(@NotNull Reference<Channel> channel) {
        channelSubscriptions.add(channel.id());
        publish(new UserEvent.SubscribedToChannel(id, channel));
    }

    @EventHandler.Multicast(queryMethod = "findUsersInChannel", paramMapping = @EventHandler.Param(from = "channel", to = "channel"))
    public void onMessageSent(MessageSent event) {
        unreadMessages.add(new UnreadMessage(Reference.fromId(event.id()), event.channel()));
    }

    @Update(path = "/unread-messages", method = "POST")
    public void markMessageAsRead(@NotNull Reference<Message> message) {
        unreadMessages.removeIf(unreadMessage -> unreadMessage.message().equals(message));
        publish(new UserEvent.MessageRead(id, message));
    }
}
