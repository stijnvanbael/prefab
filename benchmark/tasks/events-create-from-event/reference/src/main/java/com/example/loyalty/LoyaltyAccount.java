package com.example.loyalty;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.EventHandler;
import be.appify.prefab.core.annotations.rest.Filter;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.service.Reference;
import com.example.crm.Customer;
import com.example.crm.CustomerRegistered;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetList
public record LoyaltyAccount(
        @Id Reference<LoyaltyAccount> id,
        @Version long version,
        @Filter(operator = Filter.Operator.EQUAL) @NotNull Reference<Customer> customer,
        int points
) {
    @EventHandler
    public static LoyaltyAccount onCustomerRegistered(CustomerRegistered event) {
        return new LoyaltyAccount(Reference.create(), 0L, event.customer(), 0);
    }
}
