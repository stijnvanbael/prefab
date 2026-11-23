package be.appify.prefab.test.inmemory.persistence.fixture;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

/** Aggregate with a wrapper version, which is {@code null} until it is saved. */
public record Widget(@Id String id, @Version Long version, String name) {
}
