package be.appify.prefab.example.sns.user;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.domain.PublishesEvents;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
@GetById
public record User(
        @Id Reference<User> id,
        @Version long version,
        @NotNull String name
) implements PublishesEvents {
    @Create
    public User(@NotNull String name) {
        this(Reference.create(), 0L, name);
        publish(new UserEvent.Created(id, name));
    }
}
