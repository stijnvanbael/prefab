package rest.rangefilter;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
@GetList
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record Product(
        @Id String id,
        @Version long version,
        @Filter(operator = Filter.Operator.EQUAL) String name,
        @Filter(operator = Filter.Operator.GREATER_THAN_OR_EQUAL)
        @Filter(operator = Filter.Operator.LESS_THAN) BigDecimal price,
        @Filter(operator = Filter.Operator.GREATER_THAN) int stock,
        @Filter(operator = Filter.Operator.LESS_THAN_OR_EQUAL) Instant createdAt) {

    @Create
    public Product(String name, BigDecimal price, int stock) {
        this(UUID.randomUUID().toString(), 0L, name, price, stock, Instant.now());
    }
}
