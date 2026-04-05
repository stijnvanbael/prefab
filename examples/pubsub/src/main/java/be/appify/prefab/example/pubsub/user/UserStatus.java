package be.appify.prefab.example.pubsub.user;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.EventHandlerConfig;
import be.appify.prefab.core.annotations.Multicast;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.pubsub.message.Message;
import be.appify.prefab.example.pubsub.message.MessageSent;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
@GetList
@EventHandlerConfig(concurrency = "4")
public record UserStatus(
        @Id Reference<UserStatus> id,
        @Version long version,
        Reference<User> user,
        List<UnreadMessage> unreadMessages
) {
    @EventHandler
    public static UserStatus onUserCreated(UserEvent.Created event) {
        return new UserStatus(
                Reference.create(),
                0L,
                event.reference(),
                new ArrayList<>()
        );
    }

    @EventHandler
    @Multicast(
            queryMethod = "findUserStatusesInChannel",
            parameters = "channel"
    )
    public void onMessageSent(MessageSent event) {
        unreadMessages.add(new UnreadMessage(event.id(), event.channel()));
    }

    @Update(path = "/unread-messages", method = "POST")
    public void markMessageAsRead(@NotNull Reference<Message> message) {
        unreadMessages.removeIf(unreadMessage -> unreadMessage.message().equals(message));
    }
}
