package dbmigration.customidchild;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import java.util.List;

@Aggregate
@DbMigration
public record Order(
        @Id String orderId,
        @Version long version,
        @NotNull List<OrderLine> lines
) {
    public record OrderLine(
            @NotNull String sku,
            int quantity
    ) {
    }
}

