package mongodb.deferred;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

/** Uses {@code Stamp}, which another annotation processor generates, so Prefab only sees it in a later round. */
@Aggregate
@GetList
public record Late(
        @Id Reference<Late> id,
        @Version long version,
        @Filter @NotNull String label,
        @NotNull Stamp stamp
) {
    public Late(@NotNull String label, @NotNull Stamp stamp) {
        this(Reference.create(), 0L, label, stamp);
    }
}
