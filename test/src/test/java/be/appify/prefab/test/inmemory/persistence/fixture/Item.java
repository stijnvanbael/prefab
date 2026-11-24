package be.appify.prefab.test.inmemory.persistence.fixture;

import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;

/** Aggregate with a property of every kind a derived query can compare. */
public record Item(
        @Id String id,
        String name,
        String category,
        int stock,
        boolean active,
        Instant createdAt,
        List<String> tags
) {
}
