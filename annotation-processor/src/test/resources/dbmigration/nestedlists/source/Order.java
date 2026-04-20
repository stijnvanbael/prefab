package dbmigration.nestedlists;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import java.util.List;

@Aggregate
@DbMigration
public record Order(
        @Id Reference<Order> id,
        @Version long version,
        @NotNull String customerName,
        @NotNull List<OrderLine> lines
) {
    public record OrderLine(
            @NotNull String product,
            int quantity,
            @NotNull List<OrderLineNote> notes
    ) {
    }

    public record OrderLineNote(
            @NotNull String text,
            @NotNull String author
    ) {
    }
}

