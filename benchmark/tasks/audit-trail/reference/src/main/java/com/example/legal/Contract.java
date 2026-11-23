package com.example.legal;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.audit.AuditInfo;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Contract(
        @Id Reference<Contract> id,
        @Version long version,
        @NotNull String title,
        AuditInfo audit
) {
    @Create
    public Contract(@NotNull String title) {
        this(Reference.create(), 0L, title, new AuditInfo());
    }

    @Update
    public Contract rename(@NotNull String title) {
        return new Contract(id, version, title, audit);
    }
}
