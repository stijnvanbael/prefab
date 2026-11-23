package com.example.documents;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Download;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.validation.ContentType;
import be.appify.prefab.core.annotations.validation.FileSize;
import be.appify.prefab.core.domain.Binary;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Document(
        @Id Reference<Document> id,
        @Version long version,
        @NotNull String title,
        @Download @NotNull Binary file
) {
    @Create
    public Document(
            @NotNull String title,
            @NotNull @ContentType({"application/pdf", "image/png"}) @FileSize(max = 1024 * 1024) Binary file
    ) {
        this(Reference.create(), 0L, title, file);
    }
}
