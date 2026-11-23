package com.example.catalog;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Book(
        @Id Reference<Book> id,
        @Version long version,
        @NotNull @Size(max = 200) String title,
        @NotNull String author,
        @NotNull String isbn,
        int publicationYear
) {
    @Create
    public Book(@NotNull @Size(max = 200) String title, @NotNull String author, @NotNull String isbn,
            int publicationYear) {
        this(Reference.create(), 0L, title, author, isbn, publicationYear);
    }
}
