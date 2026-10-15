package rest.rangefilter.tenant;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.TenantId;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetList
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record Invoice(
        @Id String id,
        @Version long version,
        @TenantId String organisationId,
        @Filter String number,
        @Filter(operator = Filter.Operator.GREATER_THAN) long amount) {

    @Create
    public Invoice(String number, long amount) {
        this(UUID.randomUUID().toString(), 0L, null, number, amount);
    }
}
