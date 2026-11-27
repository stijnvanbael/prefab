package com.example.catalog;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@Create
@GetById
public record Book(
        @Id Reference<Book> id,
        @Version long version,
        @NotNull String title,
        @NotNull String author
) {
    public Book(@NotNull String title, @NotNull String author) {
        this(Reference.create(), 0L, title, author);
    }
}
