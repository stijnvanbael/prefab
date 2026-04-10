package terraform.source;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.service.Reference;
import org.springframework.data.annotation.Id;

@Aggregate
@DbMigration
public record UserWithRest(
        @Id Reference<UserWithRest> id,
        String name
) {
    @Create
    public UserWithRest(String name) {
        this(Reference.create(), name);
    }
}
