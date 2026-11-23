package com.example.blog;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Article(
        @Id Reference<Article> id,
        @Version long version,
        @NotNull String title,
        @NotNull String body
) {
    @Create
    public Article(@NotNull String title, @NotNull String body) {
        this(Reference.create(), 0L, title, body);
    }
}
