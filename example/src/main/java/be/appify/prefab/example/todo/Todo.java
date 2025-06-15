package be.appify.prefab.example.todo;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.DbMigration;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Search;
import be.appify.prefab.core.annotations.rest.Update;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

import static be.appify.prefab.core.annotations.rest.HttpMethod.POST;

@Aggregate
@GetById
@Search(property = "description")
@Delete
@DbMigration
public class Todo {
    @NotNull
    private final String description;
    @NotNull
    private final Boolean done;
    @NotNull
    private final Instant created;

    @Create
    public Todo(@NotNull String description) {
        this(description, false, Instant.now());
    }

    public Todo(String description, Boolean done, Instant created) {
        this.description = description;
        this.done = done;
        this.created = created;
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
        return new Todo(description, true, created);
    }

    @Update(path = "/description")
    public Todo updateDescription(@NotNull String description) {
        return new Todo(description, done, created);
    }
}
