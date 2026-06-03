package mother.staticconstants.source;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.Generate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Update;
import java.util.UUID;

import be.appify.prefab.processor.assertion.AssertionPlugin;
import be.appify.prefab.processor.dbmigration.DbMigrationPlugin;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
@Generate(plugin = AssertionPlugin.class, enabled = false)
@Generate(plugin = DbMigrationPlugin.class, enabled = false)
public record AgentTask(
        @Id String id,
        @Version long version,
        AgentRole assignedRole,
        String description) {

    @Create
    public AgentTask(AgentRole assignedRole, String description) {
        this(UUID.randomUUID().toString(), 0L, assignedRole, description);
    }

    @Update
    public AgentTask update(AgentRole assignedRole, String description) {
        return new AgentTask(id, version, assignedRole, description);
    }
}

