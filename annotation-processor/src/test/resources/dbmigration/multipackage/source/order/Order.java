package dbmigration.multipackage.order;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@DbMigration
public record Order(
        @Id Reference<Order> id,
        @Version long version,
        @NotNull String customer
) {
    public Order(@NotNull String customer) {
        this(Reference.create(), 0L, customer);
    }
}

