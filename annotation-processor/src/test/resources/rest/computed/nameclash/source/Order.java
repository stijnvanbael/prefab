package rest.computed.nameclash;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Computed;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import java.math.BigDecimal;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record Order(
        @Id Reference<Order> id,
        @Version long version,
        BigDecimal amount) {

    @Create
    public Order(BigDecimal amount) {
        this(Reference.create(), 0L, amount);
    }

    @Computed
    public BigDecimal amount() {
        return amount;
    }
}
