package be.appify.prefab.example.snssqs.user;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.PublishesEvents;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.snssqs.channel.Channel;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
@GetById
public record User(
        @Id Reference<User> id,
        @Version long version,
        @NotNull String name,
        @NotNull List<Reference<Channel>> channelSubscriptions
) implements PublishesEvents {
    @Create
    public User(@NotNull String name) {
        this(Reference.create(), 0L, name, new ArrayList<>());
        publish(new UserEvent.Created(id, name));
    }

    @Update(path = "/channel-subscriptions", method = "POST")
    public void subscribeToChannel(@NotNull Reference<Channel> channel) {
        channelSubscriptions.add(channel);
        publish(new UserEvent.SubscribedToChannel(id, channel));
    }
}
