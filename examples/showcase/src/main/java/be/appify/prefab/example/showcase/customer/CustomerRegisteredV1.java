package be.appify.prefab.example.showcase.customer;

import be.appify.prefab.core.service.Reference;

/**
 * The shape of {@link CustomerRegistered} before version 2: one name instead of a first and last name. A snapshot is
 * a plain record that only exists to read old messages; it can be deleted once the broker no longer holds them.
 *
 * @param customer the customer that registered
 * @param name     the full name of the customer
 */
public record CustomerRegisteredV1(Reference<Customer> customer, String name) {
}
