package rest.rangefilter.parent;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Parent;
import be.appify.prefab.core.service.Reference;
import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import be.appify.prefab.processor.mother.MotherPlugin;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetList
@Generate(plugin = MotherPlugin.class, enabled = false)
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record Ticket(
        @Id Reference<Ticket> id,
        @Version long version,
        @Parent Reference<Project> project,
        @Filter String title,
        @Filter(operator = Filter.Operator.LESS_THAN_OR_EQUAL) int priority) {

    @Create
    public Ticket(Reference<Project> project, String title, int priority) {
        this(Reference.create(), 0L, project, title, priority);
    }
}
