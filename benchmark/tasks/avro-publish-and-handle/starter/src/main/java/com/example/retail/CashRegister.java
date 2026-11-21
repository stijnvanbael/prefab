package com.example.retail;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record CashRegister(
        @Id Reference<CashRegister> id,
        @Version long version,
        @NotNull String name,
        double cashInDrawer
) {
    @Create
    public CashRegister(@NotNull String name) {
        this(Reference.create(), 0L, name, 0.0);
    }
}
