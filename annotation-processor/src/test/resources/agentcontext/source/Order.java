package agentcontext.source;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.Delete;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Update;
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

    @Update(path = "/rename")
    public Order rename(String customer) {
        return new Order(id, customer);
    }
}
