package com.example.shop.customer;

import be.appify.prefab.core.annotations.EventMigration;
import be.appify.prefab.core.versioning.Migration;

/**
 * Migrates {@link CustomerRegistered} from major version 1 to 2, by splitting the full name at its last space.
 *
 * <p>The migration is a plain Spring bean and a plain function, so it can be tested without a broker.</p>
 */
@EventMigration(from = "1", to = "2")
public class CustomerRegisteredMigration implements Migration<CustomerRegisteredV1, CustomerRegistered> {

    @Override
    public CustomerRegistered migrate(CustomerRegisteredV1 old) {
        var name = old.name().strip();
        var split = name.lastIndexOf(' ');
        return split < 0
                ? new CustomerRegistered(old.customer(), name, "")
                : new CustomerRegistered(old.customer(), name.substring(0, split), name.substring(split + 1));
    }
}
