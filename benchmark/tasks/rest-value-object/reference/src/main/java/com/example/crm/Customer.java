package com.example.crm;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Customer(
        @Id Reference<Customer> id,
        @Version long version,
        @NotNull String name,
        @NotNull Address address
) {
    @Create
    public Customer(@NotNull String name, @NotNull @Valid Address address) {
        this(Reference.create(), 0L, name, address);
    }

    @Update(path = "/address")
    public Customer moveTo(@NotNull @Valid Address address) {
        return new Customer(id, version, name, address);
    }
}
