package com.example.planning;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Parent;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
@GetList
public record Task(
        @Id Reference<Task> id,
        @Version long version,
        @Parent @NotNull Reference<Project> project,
        @NotNull String title,
        boolean done
) {
    @Create
    public Task(@NotNull Reference<Project> project, @NotNull String title) {
        this(Reference.create(), 0L, project, title, false);
    }

    @Update(path = "/complete", method = "POST")
    public Task complete() {
        return new Task(id, version, project, title, true);
    }
}
