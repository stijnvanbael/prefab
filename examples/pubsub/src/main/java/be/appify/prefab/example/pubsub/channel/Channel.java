package be.appify.prefab.example.pubsub.channel;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.domain.PublishesEvents;
import be.appify.prefab.example.pubsub.user.UserEvent;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;

@Aggregate
@GetList
@DbMigration
@GetById
public record Channel(
        @Id String id,
        @Version long version,
        @NotNull String name,
        @NotNull List<String> subscribers // Currently, Spring Data JDBC cannot support List<Reference<T>>
) implements PublishesEvents {
    @PersistenceCreator
    public Channel {
    }

    @Create
    public Channel(String name) {
        this(UUID.randomUUID().toString(), 0L, name, new ArrayList<>());
        publish(new ChannelCreated(id, name));
    }

    @EventHandler
    @ByReference(property = "channel")
    public void onUserSubscribed(UserEvent.SubscribedToChannel event) {
        subscribers.add(event.id());
    }
}
