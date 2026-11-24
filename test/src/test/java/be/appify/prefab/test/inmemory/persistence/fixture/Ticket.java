package be.appify.prefab.test.inmemory.persistence.fixture;

import be.appify.prefab.core.service.Reference;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

/** Aggregate identified by a {@link Reference}, like the Prefab aggregates, in a repository keyed by String. */
public record Ticket(@Id Reference<Ticket> id, @Version long version, String title) {

    public static Ticket create(String title) {
        return new Ticket(Reference.create(), 0L, title);
    }
}
