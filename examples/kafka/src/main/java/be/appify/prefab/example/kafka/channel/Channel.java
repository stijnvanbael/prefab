package be.appify.prefab.example.kafka.channel;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.ByReference;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.EventHandlerConfig;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.domain.PublishesEvents;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.kafka.user.User;
import be.appify.prefab.example.kafka.user.UserEvent;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetList
@DbMigration
@GetById
@EventHandlerConfig(concurrency = "4")
public record Channel(
        @Id Reference<Channel> id,
        @Version long version,
        String name,
        List<Reference<User>> subscribers
) implements PublishesEvents {
    @Create
    public Channel(String name) {
        this(Reference.create(), 0L, name, new ArrayList<>());
        publish(new ChannelCreated(id, name));
    }

    @EventHandler
    @ByReference(property = "channel")
    public void onUserSubscribed(UserEvent.SubscribedToChannel event) {
        subscribers.add(event.reference());
    }
}
