package be.appify.prefab.example.todo;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Security;
import be.appify.prefab.core.annotations.rest.Update;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;

import java.time.Instant;
import java.util.UUID;

import static be.appify.prefab.core.annotations.rest.HttpMethod.POST;

@Aggregate
@GetById(security = @Security(enabled = false))
@GetList(security = @Security(enabled = false))
@Delete(security = @Security(authority = "todo:delete"))
@DbMigration
public class Todo {
    @Id
    private String id;
    @Version
    private long version;
    @NotNull
    @Filter
    private final String description;
    @NotNull
    private final Boolean done;
    @NotNull
    private final Instant created;

    @Create(security = @Security(authority = "todo:create"))
    public Todo(@NotNull String description) {
        this(UUID.randomUUID().toString(), 0, description, false, Instant.now());
    }

    @PersistenceCreator
    public Todo(
            String id,
            long version,
            String description,
            Boolean done,
            Instant created
    ) {
        this.id = id;
        this.version = version;
        this.description = description;
        this.done = done;
        this.created = created;
    }

    public String id() {
        return id;
    }

    public long version() {
        return version;
    }

    public String description() {
        return description;
    }

    public Boolean done() {
        return done;
    }

    public Instant created() {
        return created;
    }

    @Update(method = POST, path = "/done")
    public Todo markDone() {
        return new Todo(id, version, description, true, created);
    }

    @Update(path = "/description", security = @Security(authority = "todo:update"))
    public Todo updateDescription(@NotNull String description) {
        return new Todo(id, version, description, done, created);
    }
}






