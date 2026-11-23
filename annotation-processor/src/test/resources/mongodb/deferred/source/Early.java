package mongodb.deferred;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetList
public record Early(
        @Id Reference<Early> id,
        @Version long version,
        @Filter @NotNull String name
) {
    public Early(@NotNull String name) {
        this(Reference.create(), 0L, name);
    }
}
