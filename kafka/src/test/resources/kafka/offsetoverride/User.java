package kafka.offsetoverride;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Create;
import be.appify.prefab.core.annotations.Id;
import be.appify.prefab.core.annotations.Version;
import be.appify.prefab.core.types.Reference;

@Aggregate
public record User(
        @Id Reference<User> id,
        @Version long version,
        String username
) {
    @Create
    public User(String username) {
        this(Reference.create(), 0L, username);
    }
}

