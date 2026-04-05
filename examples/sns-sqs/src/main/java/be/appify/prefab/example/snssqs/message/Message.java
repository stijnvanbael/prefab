package be.appify.prefab.example.snssqs.message;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.domain.PublishesEvents;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.snssqs.channel.Channel;
import be.appify.prefab.example.snssqs.user.User;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
public record Message(
        @Id Reference<Message> id,
        @Version long version,
        Reference<User> author,
        Reference<Channel> channel,
        @NotEmpty String content,
        Instant timestamp
) implements PublishesEvents {
    @Create
    public Message(
            @NotNull Reference<User> author,
            @NotNull Reference<Channel> channel,
            @NotEmpty String content
    ) {
        this(
                Reference.create(),
                0L,
                author,
                channel,
                content,
                Instant.now()
        );
        publish(new MessageSent(id, channel));
    }
}
