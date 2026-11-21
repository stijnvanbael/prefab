package com.example.notes;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
@GetList
public record Note(
        @Id Reference<Note> id,
        @Version long version,
        @Filter @NotNull String title,
        @Nullable String text,
        @Filter(operator = Filter.Operator.EQUAL) @NotNull Boolean archived
) {
    @Create
    public Note(@NotNull String title, @Nullable String text) {
        this(Reference.create(), 0L, title, text, false);
    }

    @Update(path = "/archive", method = "POST")
    public Note archive() {
        return new Note(id, version, title, text, true);
    }

    @Delete
    public void delete() {
    }
}
