package mother.staticconstants.source;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Update;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
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

