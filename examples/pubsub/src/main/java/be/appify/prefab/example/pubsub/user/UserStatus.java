package be.appify.prefab.example.pubsub.user;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.Multicast;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.pubsub.message.Message;
import be.appify.prefab.example.pubsub.message.MessageSent;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
@GetList
public record UserStatus(
        @Id String id,
        @Version long version,
        @NotNull Reference<User> user,
        @NotNull List<UnreadMessage> unreadMessages
) {
    @PersistenceCreator
    public UserStatus {
    }

    @EventHandler
    public static UserStatus onUserCreated(UserEvent.Created event) {
        return new UserStatus(
                UUID.randomUUID().toString(),
                0L,
                Reference.fromId(event.id()),
                new ArrayList<>()
        );
    }

    @EventHandler
    @Multicast(
            queryMethod = "findUserStatusesInChannel",
            paramMapping = @Multicast.Param(from = "channel", to = "channel")
    )
    public void onMessageSent(MessageSent event) {
        unreadMessages.add(new UnreadMessage(Reference.fromId(event.id()), event.channel()));
    }

    @Update(path = "/unread-messages", method = "POST")
    public void markMessageAsRead(@NotNull Reference<Message> message) {
        unreadMessages.removeIf(unreadMessage -> unreadMessage.message().equals(message));
    }
}
