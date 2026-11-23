package be.appify.prefab.example.showcase.customer;

import be.appify.prefab.core.service.Reference;
import be.appify.prefab.example.showcase.customer.application.CustomerOrderHistoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes a customer's order history only to the principal whose name is that customer's ID.
 */
@RestController
@RequestMapping("/customers/{customerId}/order-history")
class CustomerOrderHistoryAccessController {

    private final CustomerOrderHistoryRepository histories;

    CustomerOrderHistoryAccessController(CustomerOrderHistoryRepository histories) {
        this.histories = histories;
    }

    @GetMapping
    @PreAuthorize("#customerId == authentication.name")
    ResponseEntity<CustomerOrderHistory> getHistory(@PathVariable String customerId) {
        var customer = Reference.<Customer>fromId(customerId);
        return histories.findByCustomer(customer).stream()
                .findFirst()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
