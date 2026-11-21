package com.example.crm;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Customer(
        @Id Reference<Customer> id,
        @Version long version,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email String email
) {
    @Create
    public Customer(@NotBlank @Size(max = 100) String name, @NotBlank @Email String email) {
        this(Reference.create(), 0L, name, email);
    }
}
