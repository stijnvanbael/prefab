package com.example.crm;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
public record Customer(
        @Id Reference<Customer> id,
        @Version long version,
        @NotNull String name
) {
    private static final EventPublisher<CustomerRegistered> REGISTERED = EventPublisher.bind(CustomerRegistered.class);

    @Create
    public Customer(@NotNull String name) {
        this(Reference.create(), 0L, name);
        REGISTERED.publish(new CustomerRegistered(id, name));
    }
}
