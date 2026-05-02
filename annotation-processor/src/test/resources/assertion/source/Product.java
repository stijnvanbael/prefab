package assertion;

import be.appify.prefab.core.annotations.Aggregate;
import be.appify.prefab.core.annotations.rest.Create;
import be.appify.prefab.core.annotations.rest.GetById;
import be.appify.prefab.core.annotations.rest.GetList;
import be.appify.prefab.core.annotations.rest.Update;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;

@Aggregate
@GetById
@GetList
public record Product(
        @Id String id,
        @Version long version,
        String name,
        double price) {

    @Create
    public Product(String name, double price) {
        this(UUID.randomUUID().toString(), 0L, name, price);
    }

    @Update
    public Product update(String name, double price) {
        return new Product(id, version, name, price);
    }
}
