package rest.asynccreatewithparent;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.AsyncCommit;
import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Parent;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;

@Aggregate
@AsyncCommit
public record Task(
        @Id Reference<Task> id,
        @Parent Reference<Project> project,
        @NotNull String title
) {
    @Create
    public static TaskCreated create(Reference<Project> project, @NotNull String title) {
        return new TaskCreated(Reference.create(), project, title);
    }

    @Event(topic = "tasks")
    public record TaskCreated(Reference<Task> id, Reference<Project> project, String title) {
    }
}
