package com.example.crm;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.service.Reference;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Customer(
        @Id Reference<Customer> id,
        @Version long version,
        String name,
        String email
) {
    @Create
    public Customer(String name, String email) {
        this(Reference.create(), 0L, name, email);
    }
}
