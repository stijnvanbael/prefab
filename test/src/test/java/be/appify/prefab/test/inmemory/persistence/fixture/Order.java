package be.appify.prefab.test.inmemory.persistence.fixture;

import be.appify.prefab.core.service.Reference;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

/** Aggregate with a primitive version, a reference and searchable text, like a generated aggregate. */
public record Order(
        @Id String id,
        @Version long version,
        String name,
        String description,
        String category,
        Reference<Customer> customer,
        BigDecimal amount,
        List<String> tags
) {

    public static Order create(String id, String name, String category, Reference<Customer> customer, String amount) {
        return new Order(id, 0, name, null, category, customer, new BigDecimal(amount), List.of());
    }

    public Order withName(String newName) {
        return new Order(id, version, newName, description, category, customer, amount, tags);
    }

    public Order withDescription(String newDescription) {
        return new Order(id, version, name, newDescription, category, customer, amount, tags);
    }
}
