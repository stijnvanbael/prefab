package be.appify.prefab.example.mongodb.category;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.domain.PublishesEvents;
import be.appify.prefab.core.service.Reference;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

/** Represents a product category aggregate. */
@Aggregate
@GetById
@GetList
public record Category(
        @Id Reference<Category> id,
        @Version long version,
        @NotNull String name
) implements PublishesEvents {

    /** Creates a new category with the given name. */
    @Create
    public Category(String name) {
        this(Reference.create(), 0L, name);
        publish(new CategoryCreated(id, name));
    }
}
