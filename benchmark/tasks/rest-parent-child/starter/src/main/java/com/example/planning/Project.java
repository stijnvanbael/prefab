package com.example.planning;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Project(
        @Id Reference<Project> id,
        @Version long version,
        @NotNull String name
) {
    @Create
    public Project(@NotNull String name) {
        this(Reference.create(), 0L, name);
    }
}
