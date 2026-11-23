package be.appify.prefab.example.showcase.customer;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Update;
import be.appify.prefab.core.domain.EventPublisher;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

/**
 * A customer of the shop. Registering and deactivating a customer publish events that other aggregates react to.
 *
 * @param id        unique reference of the customer
 * @param version   optimistic locking version
 * @param firstName first name
 * @param lastName  last name
 * @param email     contact address
 * @param active    whether the customer can still place orders
 */
@Aggregate
@GetById
@GetList
public record Customer(
        @Id Reference<Customer> id,
        @Version long version,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        boolean active
) {
    private static final EventPublisher<CustomerRegistered> REGISTERED = EventPublisher.bind(CustomerRegistered.class);
    private static final EventPublisher<CustomerDeactivated> DEACTIVATED = EventPublisher.bind(CustomerDeactivated.class);

    /**
     * Registers a new customer and publishes {@link CustomerRegistered}.
     *
     * @param firstName first name
     * @param lastName  last name
     * @param email     contact address
     */
    @Create
    public Customer(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Email @Size(max = 255) String email
    ) {
        this(Reference.create(), 0L, firstName, lastName, email, true);
        REGISTERED.publish(new CustomerRegistered(id, firstName, lastName));
    }

    /**
     * Closes the account of the customer and publishes {@link CustomerDeactivated}.
     *
     * @return the deactivated customer
     */
    @Update(path = "/deactivation", method = "POST")
    public Customer deactivate() {
        if (!active) {
            return this;
        }
        DEACTIVATED.publish(new CustomerDeactivated(id));
        return new Customer(id, version, firstName, lastName, email, false);
    }
}
