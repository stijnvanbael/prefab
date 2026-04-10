package terraform.source;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.service.Reference;
import org.springframework.data.annotation.Id;

@Aggregate
@DbMigration
public record User(
        @Id Reference<User> id,
        String name
) {
    public User(String name) {
        this(Reference.create(), name);
    }
}
