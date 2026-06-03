package dbmigration.fkindex;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

import java.util.List;

@Aggregate
@DbMigration
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
public record Order(
        @Id Reference<Order> id,
        @Version long version,
        @NotNull String customerName,
        @NotNull List<OrderLine> lines
) {
    public Order(@NotNull String customerName) {
        this(Reference.create(), 0L, customerName, new java.util.ArrayList<>());
    }

    public record OrderLine(
            @NotNull String productId,
            int quantity
    ) {
    }
}
