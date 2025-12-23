package be.appify.prefab.example.pubsub.message;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.domain.PublishesEvents;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.pubsub.channel.Channel;
import be.appify.prefab.example.pubsub.user.User;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;

import java.time.Instant;
import java.util.UUID;

@Aggregate
@DbMigration
public record Message(
        @Id String id,
        @Version long version,
        @NotNull Reference<User> author,
        @NotNull Reference<Channel> channel,
        @NotEmpty String content,
        @NotNull Instant timestamp
) implements PublishesEvents {
    @PersistenceCreator
    public Message {
    }

    @Create
    public Message(
            @NotNull Reference<User> author,
            @NotNull Reference<Channel> channel,
            @NotEmpty String content
    ) {
        this(
                UUID.randomUUID().toString(),
                0L,
                author,
                channel,
                content,
                Instant.now()
        );
        publish(new MessageSent(id, channel));
    }
}
