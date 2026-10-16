package rest.fulltextfilter.tenant;

import be.appify.prefab.core.annotations.TenantId;
import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetList
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)public record Ticket(
        @Id String id,
        @Version long version,
        @TenantId String organisationId,
        @Filter(operator = Filter.Operator.FULL_TEXT, parameter = "query") String subject) {

    @Create
    public Ticket(String subject) {
        this(UUID.randomUUID().toString(), 0L, null, subject);
    }
}