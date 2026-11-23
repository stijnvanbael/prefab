package handwritten.source;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import org.springframework.data.annotation.Id;

@Aggregate
@GetList
@GetById
@Delete
public record Order(@Id String id, String customer) {

    @Create
    public Order(String customer) {
        this(java.util.UUID.randomUUID().toString(), customer);
    }
}
