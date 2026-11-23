package com.example.documents;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Download;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.validation.ContentType;
import be.appify.prefab.core.annotations.validation.FileSize;
import be.appify.prefab.core.domain.Binary;
import be.appify.prefab.core.problem.BadRequestException;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Document(
        @Id Reference<Document> id,
        @Version long version,
        @NotNull String title,
        @Download
        @ContentType({"application/pdf", "image/png"})
        @FileSize(max = Document.MAX_SIZE)
        @NotNull Binary file
) {
    static final int MAX_SIZE = 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("application/pdf", "image/png");

    @Create
    public Document(@NotNull String title, @NotNull Binary file) {
        this(Reference.create(), 0L, title, validated(file));
    }

    // @ContentType and @FileSize are not enforced on uploads by the generated controller (yet), so check here too.
    private static Binary validated(Binary file) {
        if (file == null || !ALLOWED_TYPES.contains(file.contentType())) {
            throw new BadRequestException("Only PDF and PNG files are accepted");
        }
        if (file.data().length() > MAX_SIZE) {
            throw new BadRequestException("Files may be at most 1 MB");
        }
        return file;
    }
}
