package be.appify.prefab.example.kafka.user;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.kafka.channel.Channel;
import be.appify.prefab.example.kafka.message.Message;
import be.appify.prefab.example.kafka.message.MessageSent;
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
        @NotNull List<String> channelSubscriptions,
        @NotNull List<UnreadMessage> unreadMessages
) {
    @PersistenceCreator
    public User {
    }

    @Create
    public User(@NotNull String name) {
        this(UUID.randomUUID().toString(), 0L, name, new ArrayList<>(), new ArrayList<>());
    }

    @Update(path = "/channel-subscriptions", method = "POST")
    public void subscribeToChannel(@NotNull Reference<Channel> channel) {
        channelSubscriptions.add(channel.id());
    }

    @EventHandler.Multicast(queryMethod = "findUsersInChannel", paramMapping = @EventHandler.Param(from = "channel", to = "channel"))
    public void onMessageSent(MessageSent event) {
        unreadMessages.add(new UnreadMessage(Reference.fromId(event.id()), event.channel()));
    }

    @Update(path = "/unread-messages", method = "POST")
    public void markMessageAsRead(@NotNull Reference<Message> message) {
        unreadMessages.removeIf(unreadMessage -> unreadMessage.message().equals(message));
    }
}
